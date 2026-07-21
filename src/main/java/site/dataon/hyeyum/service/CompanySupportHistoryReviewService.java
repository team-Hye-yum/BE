package site.dataon.hyeyum.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import site.dataon.hyeyum.common.MetricUnits;
import site.dataon.hyeyum.common.MoneyUnits;
import site.dataon.hyeyum.common.PatentRegistrationStatuses;
import site.dataon.hyeyum.common.SupportSelectionResults;
import site.dataon.hyeyum.domain.BtpSupportHistory;
import site.dataon.hyeyum.domain.CompanyEmploymentStatistics;
import site.dataon.hyeyum.domain.CompanyFinancialStatistics;
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
import site.dataon.hyeyum.dto.SupportHistoryPostSupportChangeResponse;
import site.dataon.hyeyum.dto.SupportHistoryPostSupportChangeResponse.ChangeObservationItem;
import site.dataon.hyeyum.dto.SupportHistoryPostSupportChangeResponse.ObservationStatus;
import site.dataon.hyeyum.dto.SupportHistoryPostSupportChangeResponse.TopChangeItem;
import site.dataon.hyeyum.repository.BtpSupportHistoryRepository;
import site.dataon.hyeyum.repository.CompanyEmploymentStatisticsRepository;
import site.dataon.hyeyum.repository.CompanyFinancialStatisticsRepository;
import site.dataon.hyeyum.repository.CompanyNtisCollaborativeProjectRepository;
import site.dataon.hyeyum.repository.CompanyNtisLeadProjectRepository;
import site.dataon.hyeyum.repository.CompanyPatentRepository;
import site.dataon.hyeyum.repository.CompanyRepository;
import site.dataon.hyeyum.repository.SupportYearTypeCountProjection;

@Service
public class CompanySupportHistoryReviewService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String EMPTY_MESSAGE = "비교 가능한 과거 지원 이력 없음";
    private static final String POST_SUPPORT_EMPTY_MESSAGE = "지원 이후 변화를 확인할 부산TP 지원 이력이 없음";
    private static final int MAX_COMPARISON_COUNT = 2;
    private static final int MAX_TOP_CHANGE_COUNT = 3;

    private final CompanyRepository companyRepository;
    private final BtpSupportHistoryRepository supportHistoryRepository;
    private final CompanyFinancialStatisticsRepository financialStatisticsRepository;
    private final CompanyEmploymentStatisticsRepository employmentStatisticsRepository;
    private final CompanyPatentRepository patentRepository;
    private final CompanyNtisLeadProjectRepository ntisLeadProjectRepository;
    private final CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository;

    public CompanySupportHistoryReviewService(
            CompanyRepository companyRepository,
            BtpSupportHistoryRepository supportHistoryRepository,
            CompanyFinancialStatisticsRepository financialStatisticsRepository,
            CompanyEmploymentStatisticsRepository employmentStatisticsRepository,
            CompanyPatentRepository patentRepository,
            CompanyNtisLeadProjectRepository ntisLeadProjectRepository,
            CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository) {
        this.companyRepository = companyRepository;
        this.supportHistoryRepository = supportHistoryRepository;
        this.financialStatisticsRepository = financialStatisticsRepository;
        this.employmentStatisticsRepository = employmentStatisticsRepository;
        this.patentRepository = patentRepository;
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
                supportHistoryRepository.countByCompanyIdAndSelectionResult(companyId, SupportSelectionResults.SELECTED),
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

    @Transactional(readOnly = true)
    public ApiDataResponse<SupportHistoryPostSupportChangeResponse> postSupportChanges(Integer companyId) {
        verifyCompanyExists(companyId);

        List<BtpSupportHistory> selectedHistories =
                selectedOnly(supportHistoryRepository.findByCompanyIdOrderBySupportYearAscSupportHistoryIdAsc(companyId));
        Map<Integer, List<BtpSupportHistory>> historiesByEndYear = selectedHistories.stream()
                .filter(history -> supportEndYear(history) != null)
                .collect(Collectors.groupingBy(this::supportEndYear, LinkedHashMap::new, Collectors.toList()));

        if (historiesByEndYear.isEmpty()) {
            return new ApiDataResponse<>(
                    new SupportHistoryPostSupportChangeResponse(List.of(), POST_SUPPORT_EMPTY_MESSAGE));
        }

        Map<Integer, CompanyFinancialStatistics> financialsByYear = financialStatisticsRepository
                .findByCompanyIdOrderByYearAsc(companyId)
                .stream()
                .collect(Collectors.toMap(CompanyFinancialStatistics::getYear, Function.identity(), (left, right) -> right));
        Map<Integer, CompanyEmploymentStatistics> employmentsByYear = employmentStatisticsRepository
                .findByCompanyIdOrderByYearAsc(companyId)
                .stream()
                .collect(Collectors.toMap(CompanyEmploymentStatistics::getYear, Function.identity(), (left, right) -> right));
        Integer maxKodataYear = maxComparableKodataYear();

        List<ChangeObservationItem> observations = historiesByEndYear.entrySet().stream()
                .map(entry -> postSupportObservation(
                        companyId,
                        entry.getKey(),
                        entry.getValue(),
                        financialsByYear,
                        employmentsByYear,
                        maxKodataYear))
                .toList();

        return new ApiDataResponse<>(new SupportHistoryPostSupportChangeResponse(observations, null));
    }

    private ChangeObservationItem postSupportObservation(
            Integer companyId,
            Integer supportEndYear,
            List<BtpSupportHistory> histories,
            Map<Integer, CompanyFinancialStatistics> financialsByYear,
            Map<Integer, CompanyEmploymentStatistics> employmentsByYear,
            Integer maxKodataYear) {
        Integer observationYear = supportEndYear + 1;
        ObservationStatus status = observationStatus(
                observationYear,
                financialsByYear.get(observationYear),
                employmentsByYear.get(observationYear),
                registeredPatentCount(companyId, observationYear),
                ntisProjectCount(companyId, observationYear),
                maxKodataYear);
        String titleText = supportEndYear + "년 지원 " + histories.size() + "건 종료 (->" + observationYear + ")";
        String descriptionText = postSupportDescription(
                status,
                companyId,
                supportEndYear,
                observationYear,
                financialsByYear.get(supportEndYear),
                financialsByYear.get(observationYear),
                employmentsByYear.get(supportEndYear),
                employmentsByYear.get(observationYear));
        List<TopChangeItem> topChanges = topChanges(
                companyId,
                supportEndYear,
                observationYear,
                financialsByYear.get(supportEndYear),
                financialsByYear.get(observationYear),
                employmentsByYear.get(supportEndYear),
                employmentsByYear.get(observationYear));

        return new ChangeObservationItem(
                supportEndYear,
                observationYear,
                histories.size(),
                titleText,
                descriptionText,
                topChanges,
                status);
    }

    private ObservationStatus observationStatus(
            Integer observationYear,
            CompanyFinancialStatistics financial,
            CompanyEmploymentStatistics employment,
            int registeredPatentCount,
            int ntisProjectCount,
            Integer maxKodataYear) {
        if (maxKodataYear == null || observationYear == null || observationYear > maxKodataYear) {
            return new ObservationStatus("PENDING", "아직 관찰 불가");
        }
        int presentCount = 0;
        if (financial != null && financial.getSalesAmount() != null) {
            presentCount++;
        }
        if (employment != null && employment.getEmployeeCount() != null) {
            presentCount++;
        }
        if (registeredPatentCount > 0) {
            presentCount++;
        }
        if (ntisProjectCount > 0) {
            presentCount++;
        }
        if (presentCount >= 3) {
            return new ObservationStatus("AVAILABLE", "관찰 가능");
        }
        if (presentCount > 0) {
            return new ObservationStatus("PARTIAL_AVAILABLE", "일부 확인 가능");
        }
        return new ObservationStatus("PENDING", "아직 관찰 불가");
    }

    private String postSupportDescription(
            ObservationStatus status,
            Integer companyId,
            Integer supportEndYear,
            Integer observationYear,
            CompanyFinancialStatistics beforeFinancial,
            CompanyFinancialStatistics afterFinancial,
            CompanyEmploymentStatistics beforeEmployment,
            CompanyEmploymentStatistics afterEmployment) {
        if ("PENDING".equals(status.code())) {
            return "다음 데이터 갱신 후 확인 예정";
        }
        return "매출 " + salesText(beforeFinancial) + " -> " + salesText(afterFinancial)
                + " · 종업원 " + employeeText(beforeEmployment) + " -> " + employeeText(afterEmployment)
                + " · 특허 " + registeredPatentCount(companyId, supportEndYear)
                + " -> " + registeredPatentCount(companyId, observationYear) + "건";
    }

    private List<TopChangeItem> topChanges(
            Integer companyId,
            Integer supportEndYear,
            Integer observationYear,
            CompanyFinancialStatistics beforeFinancial,
            CompanyFinancialStatistics afterFinancial,
            CompanyEmploymentStatistics beforeEmployment,
            CompanyEmploymentStatistics afterEmployment) {
        Integer maxKodataYear = maxComparableKodataYear();
        if (observationYear == null || maxKodataYear == null || observationYear > maxKodataYear) {
            return List.of();
        }

        List<TopChangeCandidate> candidates = new ArrayList<>();
        addTopChange(candidates, "SALES_AMOUNT", "매출",
                value(beforeFinancial, CompanyFinancialStatistics::getSalesAmount),
                value(afterFinancial, CompanyFinancialStatistics::getSalesAmount),
                MoneyUnits.KRW_THOUSAND,
                MoneyUnits.KRW_THOUSAND,
                this::formatThousandWon);
        addTopChange(candidates, "OPERATING_MARGIN", "영업이익률",
                value(beforeFinancial, CompanyFinancialStatistics::getOperatingMargin),
                value(afterFinancial, CompanyFinancialStatistics::getOperatingMargin),
                MetricUnits.PERCENT,
                MetricUnits.PERCENT_POINT,
                this::formatPercent);
        addTopChange(candidates, "EMPLOYEE_COUNT", "종업원",
                value(beforeEmployment, CompanyEmploymentStatistics::getEmployeeCount),
                value(afterEmployment, CompanyEmploymentStatistics::getEmployeeCount),
                MetricUnits.COUNT,
                MetricUnits.COUNT,
                value -> formatInteger(value) + "명");
        addTopChange(candidates, "RESEARCH_AND_DEVELOPMENT_EXPENSE", "연구개발비",
                value(beforeFinancial, CompanyFinancialStatistics::getResearchAndDevelopmentExpense),
                value(afterFinancial, CompanyFinancialStatistics::getResearchAndDevelopmentExpense),
                MoneyUnits.KRW_THOUSAND,
                MoneyUnits.KRW_THOUSAND,
                this::formatThousandWon);
        addTopChange(candidates, "REGISTERED_PATENT_COUNT", "특허",
                registeredPatentCount(companyId, supportEndYear),
                registeredPatentCount(companyId, observationYear),
                MetricUnits.COUNT,
                MetricUnits.COUNT,
                value -> formatInteger(value) + "건");
        addTopChange(candidates, "NTIS_PROJECT_COUNT", "국가 R&D",
                ntisProjectCount(companyId, supportEndYear),
                ntisProjectCount(companyId, observationYear),
                MetricUnits.COUNT,
                MetricUnits.COUNT,
                value -> formatInteger(value) + "건");

        return candidates.stream()
                .sorted(Comparator.comparing(TopChangeCandidate::absoluteChangeRate).reversed())
                .limit(MAX_TOP_CHANGE_COUNT)
                .map(TopChangeCandidate::item)
                .toList();
    }

    private <T> Number value(T source, Function<T, ? extends Number> getter) {
        return source == null ? null : getter.apply(source);
    }

    private void addTopChange(
            List<TopChangeCandidate> candidates,
            String metric,
            String label,
            Number beforeValue,
            Number afterValue,
            String valueUnit,
            String changeValueUnit,
            Function<Number, String> formatter) {
        if (beforeValue == null || afterValue == null || beforeValue.doubleValue() == 0.0d) {
            return;
        }

        double before = beforeValue.doubleValue();
        double after = afterValue.doubleValue();
        double change = after - before;
        double changeRate = round2(change / Math.abs(before) * 100.0d);
        Number changeValue;
        if (integerLike(beforeValue) && integerLike(afterValue)) {
            changeValue = Integer.valueOf((int) Math.round(change));
        } else {
            changeValue = Double.valueOf(round2(change));
        }
        String displayText = label + " " + formatter.apply(beforeValue) + " -> " + formatter.apply(afterValue)
                + " (" + changeDisplay(changeValue, changeValueUnit, changeRate) + ")";
        TopChangeItem item = new TopChangeItem(
                metric,
                label,
                beforeValue,
                afterValue,
                valueUnit,
                changeValue,
                changeValueUnit,
                changeRate,
                MetricUnits.PERCENT,
                displayText);
        candidates.add(new TopChangeCandidate(item, Math.abs(changeRate)));
    }

    private boolean integerLike(Number value) {
        return value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long;
    }

    private String signedPercent(double value) {
        if (value > 0) {
            return "+" + formatOneDecimal(value) + "%";
        }
        return formatOneDecimal(value) + "%";
    }

    private String changeDisplay(Number changeValue, String changeValueUnit, double changeRate) {
        if (MetricUnits.PERCENT_POINT.equals(changeValueUnit)) {
            return signedPercentPoint(changeValue.doubleValue()) + ", " + signedPercent(changeRate);
        }
        return signedPercent(changeRate);
    }

    private String signedPercentPoint(double value) {
        if (value > 0) {
            return "+" + formatOneDecimal(value) + "%p";
        }
        return formatOneDecimal(value) + "%p";
    }

    private String formatThousandWon(Number value) {
        if (value == null) {
            return "데이터 없음";
        }
        return String.format(java.util.Locale.KOREA, "%,d천원", Math.round(value.doubleValue()));
    }

    private String formatPercent(Number value) {
        if (value == null) {
            return "데이터 없음";
        }
        return formatOneDecimal(value.doubleValue()) + "%";
    }

    private String formatInteger(Number value) {
        return String.format(java.util.Locale.KOREA, "%,d", Math.round(value.doubleValue()));
    }

    private double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private String formatOneDecimal(double value) {
        return String.format(java.util.Locale.KOREA, "%.1f", value);
    }

    private String salesText(CompanyFinancialStatistics statistics) {
        if (statistics == null || statistics.getSalesAmount() == null) {
            return "데이터 없음";
        }
        return String.format(java.util.Locale.KOREA, "%.1f억", statistics.getSalesAmount() / 100000.0);
    }

    private String employeeText(CompanyEmploymentStatistics statistics) {
        if (statistics == null || statistics.getEmployeeCount() == null) {
            return "데이터 없음";
        }
        return statistics.getEmployeeCount() + "명";
    }

    private int registeredPatentCount(Integer companyId, Integer year) {
        if (year == null) {
            return 0;
        }
        return patentRepository.countRegisteredActivePatentsUntil(
                companyId, PatentRegistrationStatuses.REGISTERED, LocalDate.of(year + 1, 1, 1));
    }

    private int ntisProjectCount(Integer companyId, Integer year) {
        if (year == null) {
            return 0;
        }
        return ntisLeadProjectRepository.countByCompanyIdAndReferenceYear(companyId, year)
                + ntisCollaborativeProjectRepository.countByCompanyIdAndReferenceYear(companyId, year);
    }

    private Integer maxComparableKodataYear() {
        Integer financialMaxYear = financialStatisticsRepository.findMaxYear();
        Integer employmentMaxYear = employmentStatisticsRepository.findMaxYear();
        if (financialMaxYear == null) {
            return employmentMaxYear;
        }
        if (employmentMaxYear == null) {
            return financialMaxYear;
        }
        return Math.min(financialMaxYear, employmentMaxYear);
    }

    private Integer supportEndYear(BtpSupportHistory history) {
        if (history.getEndDate() != null) {
            return history.getEndDate().getYear();
        }
        return history.getSupportYear();
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
                .filter(history -> sameText(history.getSelectionResult(), SupportSelectionResults.SELECTED))
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
        return new MoneyValue(value, MoneyUnits.KRW_THOUSAND);
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : BASIC_DATE.format(date);
    }

    private record ComparisonCandidate(
            ComparisonItem item, Integer pastSupportYear, LocalDate pastSelectedDate, Long pastSupportHistoryId) {}

    private record TopChangeCandidate(TopChangeItem item, double absoluteChangeRate) {}
}
