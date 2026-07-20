package site.dataon.hyeyum.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import site.dataon.hyeyum.domain.BtpSupportHistory;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.MoneyValue;
import site.dataon.hyeyum.dto.SupportHistoryLatestVsPastResponse;
import site.dataon.hyeyum.dto.SupportHistoryLatestVsPastResponse.BtpSupportTimeline;
import site.dataon.hyeyum.dto.SupportHistoryLatestVsPastResponse.BtpSupportTimelineItem;
import site.dataon.hyeyum.dto.SupportHistoryLatestVsPastResponse.ComparisonItem;
import site.dataon.hyeyum.dto.SupportHistoryLatestVsPastResponse.ReviewSignals;
import site.dataon.hyeyum.dto.SupportHistoryLatestVsPastResponse.Summary;
import site.dataon.hyeyum.dto.SupportHistoryLatestVsPastResponse.SupportHistoryCompareItem;
import site.dataon.hyeyum.dto.SupportHistoryLatestVsPastResponse.YearlySupportChart;
import site.dataon.hyeyum.dto.SupportHistoryLatestVsPastResponse.YearlySupportCountItem;
import site.dataon.hyeyum.repository.BtpSupportHistoryRepository;
import site.dataon.hyeyum.repository.CompanyNtisCollaborativeProjectRepository;
import site.dataon.hyeyum.repository.CompanyNtisLeadProjectRepository;
import site.dataon.hyeyum.repository.CompanyRepository;
import site.dataon.hyeyum.repository.SupportYearTypeCountProjection;

@Service
public class CompanySupportHistoryReviewService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String KRW_THOUSAND = "KRW_THOUSAND";
    private static final String SELECTED_RESULT = "지원대상";
    private static final String EMPTY_MESSAGE = "비교 가능한 과거 지원 이력 없음";
    private static final int MAX_COMPARISON_COUNT = 2;

    private final CompanyRepository companyRepository;
    private final BtpSupportHistoryRepository supportHistoryRepository;
    private final CompanyNtisLeadProjectRepository ntisLeadProjectRepository;
    private final CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository;

    public CompanySupportHistoryReviewService(
            CompanyRepository companyRepository,
            BtpSupportHistoryRepository supportHistoryRepository,
            CompanyNtisLeadProjectRepository ntisLeadProjectRepository,
            CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository) {
        this.companyRepository = companyRepository;
        this.supportHistoryRepository = supportHistoryRepository;
        this.ntisLeadProjectRepository = ntisLeadProjectRepository;
        this.ntisCollaborativeProjectRepository = ntisCollaborativeProjectRepository;
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<SupportHistoryLatestVsPastResponse> latestVsPast(Integer companyId) {
        verifyCompanyExists(companyId);

        Integer latestSupportYear = supportHistoryRepository.findMaxSupportYear(companyId);
        List<BtpSupportHistory> timeline = supportHistoryRepository.findTimeline(companyId);
        YearlySupportChart yearlySupportChart = new YearlySupportChart(supportHistoryRepository
                .countByYearAndSupportType(companyId)
                .stream()
                .map(this::mapYearlySupportCountItem)
                .toList());
        BtpSupportTimeline btpSupportTimeline = new BtpSupportTimeline(timeline.stream()
                .map(this::mapTimelineItem)
                .toList());

        if (latestSupportYear == null) {
            Summary summary = new Summary(0, 0, nationalRndCount(companyId), 0);
            return new ApiDataResponse<>(new SupportHistoryLatestVsPastResponse(
                    summary, yearlySupportChart, btpSupportTimeline, List.of(), List.of(), EMPTY_MESSAGE));
        }

        List<BtpSupportHistory> latestTargets =
                supportHistoryRepository.findByCompanyIdAndSupportYearOrderBySelectedDateDescSupportHistoryIdDesc(
                        companyId, latestSupportYear);
        List<BtpSupportHistory> pastSupports = supportHistoryRepository
                .findByCompanyIdAndSupportYearLessThanOrderBySupportYearDescSelectedDateDescSupportHistoryIdDesc(
                        companyId, latestSupportYear);
        List<BtpSupportHistory> selectedLatestTargets = selectedOnly(latestTargets);
        List<BtpSupportHistory> effectiveLatestTargets =
                selectedLatestTargets.isEmpty() ? latestTargets : selectedLatestTargets;
        List<BtpSupportHistory> selectedPastSupports = selectedOnly(pastSupports);

        List<ComparisonCandidate> candidates = effectiveLatestTargets.stream()
                .flatMap(latest -> selectedPastSupports.stream().map(past -> candidate(latest, past)))
                .sorted(candidateComparator())
                .limit(MAX_COMPARISON_COUNT)
                .toList();

        List<ComparisonItem> comparisons = candidates.stream().map(ComparisonCandidate::item).toList();
        Summary summary = new Summary(
                supportHistoryRepository.countByCompanyIdAndSelectionResult(companyId, SELECTED_RESULT),
                effectiveLatestTargets.size(),
                nationalRndCount(companyId),
                comparisons.size());
        String emptyMessage = comparisons.isEmpty() ? EMPTY_MESSAGE : null;
        return new ApiDataResponse<>(new SupportHistoryLatestVsPastResponse(
                summary,
                yearlySupportChart,
                btpSupportTimeline,
                effectiveLatestTargets.stream().map(this::mapCompareItem).toList(),
                comparisons,
                emptyMessage));
    }

    private void verifyCompanyExists(Integer companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "기업 정보를 찾을 수 없습니다.");
        }
    }

    private int nationalRndCount(Integer companyId) {
        return ntisLeadProjectRepository.countByCompanyId(companyId)
                + ntisCollaborativeProjectRepository.countByCompanyId(companyId);
    }

    private List<BtpSupportHistory> selectedOnly(List<BtpSupportHistory> histories) {
        return histories.stream()
                .filter(history -> sameText(history.getSelectionResult(), SELECTED_RESULT))
                .toList();
    }

    private YearlySupportCountItem mapYearlySupportCountItem(SupportYearTypeCountProjection projection) {
        return new YearlySupportCountItem(
                projection.getSupportYear(),
                projection.getSupportType(),
                projection.getSupportCount() == null ? 0 : projection.getSupportCount());
    }

    private BtpSupportTimelineItem mapTimelineItem(BtpSupportHistory history) {
        return new BtpSupportTimelineItem(
                history.getSupportHistoryId(),
                history.getSupportYear(),
                history.getCode(),
                history.getBudgetProgramName(),
                history.getSupportType(),
                history.getSupportCategory(),
                history.getSupportDetail(),
                history.getSupportItem(),
                formatDate(history.getSelectedDate()),
                history.getSelectionResult(),
                money(history.getSupportAmount()),
                formatDate(history.getStartDate()),
                formatDate(history.getEndDate()));
    }

    private SupportHistoryCompareItem mapCompareItem(BtpSupportHistory history) {
        return new SupportHistoryCompareItem(
                history.getSupportHistoryId(),
                history.getSupportYear(),
                history.getBudgetProgramName(),
                history.getSupportType(),
                history.getSupportItem(),
                formatDate(history.getStartDate()),
                formatDate(history.getEndDate()),
                money(history.getSupportAmount()),
                history.getSelectionResult());
    }

    private ComparisonCandidate candidate(BtpSupportHistory latest, BtpSupportHistory past) {
        ReviewSignals signals = reviewSignals(latest, past);
        ComparisonItem item = new ComparisonItem(
                comparisonId(latest, past),
                mapCompareItem(latest),
                mapCompareItem(past),
                signals,
                reviewComment(signals));
        return new ComparisonCandidate(item, past.getSupportYear(), past.getSelectedDate(), past.getSupportHistoryId());
    }

    private ReviewSignals reviewSignals(BtpSupportHistory latest, BtpSupportHistory past) {
        boolean sameSupportType = sameText(latest.getSupportType(), past.getSupportType());
        boolean sameSupportItem = sameText(latest.getSupportItem(), past.getSupportItem());
        return new ReviewSignals(
                sameSupportType,
                sameSupportItem,
                differentPresentText(latest.getSupportType(), past.getSupportType()),
                continuousSupportWithin12Months(
                        latest.getStartDate(), latest.getEndDate(), past.getStartDate(), past.getEndDate()));
    }

    private boolean continuousSupportWithin12Months(
            LocalDate currentStartDate, LocalDate currentEndDate, LocalDate pastStartDate, LocalDate pastEndDate) {
        if (currentStartDate == null || currentEndDate == null || pastStartDate == null || pastEndDate == null) {
            return false;
        }
        if (currentEndDate.isBefore(pastStartDate)) {
            return !pastStartDate.isAfter(currentEndDate.plusMonths(12));
        }
        if (pastEndDate.isBefore(currentStartDate)) {
            return !currentStartDate.isAfter(pastEndDate.plusMonths(12));
        }
        return true;
    }

    private Comparator<ComparisonCandidate> candidateComparator() {
        return Comparator.comparing((ComparisonCandidate candidate) -> candidate.item().reviewSignals().sameSupportType())
                .reversed()
                .thenComparing(candidate -> candidate.item().reviewSignals().sameSupportItem(), Comparator.reverseOrder())
                .thenComparing(
                        candidate -> candidate.item().reviewSignals().continuousSupportWithin12Months(),
                        Comparator.reverseOrder())
                .thenComparing(ComparisonCandidate::pastSupportYear, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ComparisonCandidate::pastSelectedDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ComparisonCandidate::pastSupportHistoryId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private String reviewComment(ReviewSignals signals) {
        if (signals.sameSupportType() && signals.sameSupportItem() && signals.continuousSupportWithin12Months()) {
            return "사업 유형과 지원 품목이 동일하며, 과거 지원 종료 후 12개월 이내에 후속 지원이 확인되어 연속 지원 여부 확인이 필요합니다.";
        }
        if (signals.sameSupportType() && signals.sameSupportItem()) {
            return "사업 유형과 지원 품목이 동일하여 동일·유사 지원 여부 확인이 필요합니다.";
        }
        if (signals.sameSupportType() && signals.continuousSupportWithin12Months()) {
            return "사업 유형이 동일하며, 과거 지원 종료 후 12개월 이내에 후속 지원이 확인되어 연속 지원 여부 확인이 필요합니다.";
        }
        if (signals.sameSupportType()) {
            return "사업 유형이 동일하여 유사 지원 여부 확인이 필요합니다.";
        }
        if (signals.supportTypeLinkagePossible()) {
            return "사업 유형은 다르지만 지원 이력 간 연계 가능성이 있어 후속 지원 여부 확인이 필요합니다.";
        }
        return "최신 지원 이력과 과거 지원 이력의 사업 범위가 겹치는지 확인이 필요합니다.";
    }

    private String comparisonId(BtpSupportHistory latest, BtpSupportHistory past) {
        return latest.getSupportHistoryId() + "-" + past.getSupportHistoryId();
    }

    private boolean sameText(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return !normalizedLeft.isBlank() && Objects.equals(normalizedLeft, normalizedRight);
    }

    private boolean differentPresentText(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return !normalizedLeft.isBlank() && !normalizedRight.isBlank() && !Objects.equals(normalizedLeft, normalizedRight);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private MoneyValue money(Integer value) {
        return new MoneyValue(value, KRW_THOUSAND);
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : BASIC_DATE.format(date);
    }

    private record ComparisonCandidate(
            ComparisonItem item, Integer pastSupportYear, LocalDate pastSelectedDate, Long pastSupportHistoryId) {}
}
