package site.dataon.hyeyum.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import site.dataon.hyeyum.common.PatentRegistrationStatuses;
import site.dataon.hyeyum.domain.BtpSupportHistory;
import site.dataon.hyeyum.domain.Company;
import site.dataon.hyeyum.domain.CompanyBusinessPurpose;
import site.dataon.hyeyum.domain.CompanyEmploymentStatistics;
import site.dataon.hyeyum.domain.CompanyFinancialStatistics;
import site.dataon.hyeyum.domain.CompanyNtisCollaborativeProject;
import site.dataon.hyeyum.domain.CompanyNtisLeadProject;
import site.dataon.hyeyum.domain.CompanyPatent;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.Capabilities;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.Employment;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.Financials;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.Options;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.PatentSummary;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.Profile;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.ResearchOrganizations;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse.SupportHistory;
import site.dataon.hyeyum.repository.BtpSupportHistoryRepository;
import site.dataon.hyeyum.repository.CompanyBusinessPurposeRepository;
import site.dataon.hyeyum.repository.CompanyEmploymentStatisticsRepository;
import site.dataon.hyeyum.repository.CompanyFinancialStatisticsRepository;
import site.dataon.hyeyum.repository.CompanyNtisCollaborativeProjectRepository;
import site.dataon.hyeyum.repository.CompanyNtisLeadProjectRepository;
import site.dataon.hyeyum.repository.CompanyPatentRepository;
import site.dataon.hyeyum.repository.CompanyRepository;

@Service
public class CompanyAiAnalysisPayloadService {

    private static final List<String> SELECTED_RESULTS = List.of("지원대상", "선정");
    private static final List<String> MARKET_KEYWORDS =
            List.of("전시", "수출", "마케팅", "판로", "해외", "브랜드", "홍보", "시장", "바이어");
    private static final List<String> TECH_RND_KEYWORDS =
            List.of("기술", "R&D", "연구", "개발", "시제품", "장비", "시험", "인증", "특허");
    private static final List<String> JOB_CREATION_KEYWORDS =
            List.of("일자리", "고용", "인건비", "채용", "인력");

    private final CompanyRepository companyRepository;
    private final BtpSupportHistoryRepository supportHistoryRepository;
    private final CompanyEmploymentStatisticsRepository employmentStatisticsRepository;
    private final CompanyFinancialStatisticsRepository financialStatisticsRepository;
    private final CompanyBusinessPurposeRepository businessPurposeRepository;
    private final CompanyPatentRepository patentRepository;
    private final CompanyNtisLeadProjectRepository ntisLeadProjectRepository;
    private final CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository;

    public CompanyAiAnalysisPayloadService(
            CompanyRepository companyRepository,
            BtpSupportHistoryRepository supportHistoryRepository,
            CompanyEmploymentStatisticsRepository employmentStatisticsRepository,
            CompanyFinancialStatisticsRepository financialStatisticsRepository,
            CompanyBusinessPurposeRepository businessPurposeRepository,
            CompanyPatentRepository patentRepository,
            CompanyNtisLeadProjectRepository ntisLeadProjectRepository,
            CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository) {
        this.companyRepository = companyRepository;
        this.supportHistoryRepository = supportHistoryRepository;
        this.employmentStatisticsRepository = employmentStatisticsRepository;
        this.financialStatisticsRepository = financialStatisticsRepository;
        this.businessPurposeRepository = businessPurposeRepository;
        this.patentRepository = patentRepository;
        this.ntisLeadProjectRepository = ntisLeadProjectRepository;
        this.ntisCollaborativeProjectRepository = ntisCollaborativeProjectRepository;
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<CompanyAiAnalysisPayloadResponse> payload(Integer companyId) {
        Company company = companyRepository
                .findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found."));
        List<BtpSupportHistory> supportHistories =
                supportHistoryRepository.findByCompanyIdOrderBySupportYearAscSupportHistoryIdAsc(companyId);
        List<BtpSupportHistory> selectedHistories = supportHistories.stream()
                .filter(this::isSelected)
                .toList();
        List<CompanyEmploymentStatistics> employmentStatistics =
                employmentStatisticsRepository.findByCompanyIdOrderByYearAsc(companyId);
        List<CompanyFinancialStatistics> financialStatistics =
                financialStatisticsRepository.findByCompanyIdOrderByYearAsc(companyId);
        List<CompanyBusinessPurpose> businessPurposes =
                businessPurposeRepository.findByCompanyIdOrderByDisplayOrderAscBusinessPurposeIdAsc(companyId);
        List<CompanyNtisLeadProject> leadProjects =
                ntisLeadProjectRepository.findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisLeadProjectIdAsc(companyId);
        List<CompanyNtisCollaborativeProject> collaborativeProjects =
                ntisCollaborativeProjectRepository
                        .findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisCollaborativeProjectIdAsc(companyId);

        CompanyAiAnalysisPayloadResponse response = new CompanyAiAnalysisPayloadResponse(
                company.getCompanyId(),
                profile(company),
                capabilities(company, businessPurposes, leadProjects, collaborativeProjects),
                financials(company, financialStatistics),
                employment(employmentStatistics),
                supportHistory(supportHistories, selectedHistories),
                new Options(3));
        return new ApiDataResponse<>(response);
    }

    private Profile profile(Company company) {
        return new Profile(
                company.getIndustryName(),
                company.getIndustryBrief(),
                company.getKsicCode(),
                company.getRegionName(),
                company.getEstablishedDate(),
                company.getCompanySize(),
                company.getMainProduct());
    }

    private Capabilities capabilities(
            Company company,
            List<CompanyBusinessPurpose> businessPurposes,
            List<CompanyNtisLeadProject> leadProjects,
            List<CompanyNtisCollaborativeProject> collaborativeProjects) {
        List<String> purposes = businessPurposes.stream()
                .map(CompanyBusinessPurpose::getBusinessPurpose)
                .filter(this::hasText)
                .limit(5)
                .toList();
        List<String> ntisProjectNames = leadProjects.stream()
                .map(CompanyNtisLeadProject::getProjectName)
                .filter(this::hasText)
                .limit(5)
                .toList();
        int ntisProjectCount = leadProjects.size() + collaborativeProjects.size();
        return new Capabilities(
                purposes,
                ntisProjectNames,
                ntisProjectCount,
                patentSummary(company.getCompanyId()),
                new ResearchOrganizations(
                        company.getHasResearchLab(),
                        company.getHasRndDepartment(),
                        company.getResearcherCount()));
    }

    private Financials financials(Company company, List<CompanyFinancialStatistics> financialStatistics) {
        CompanyFinancialStatistics latest = financialStatistics.stream()
                .filter(stat -> stat.getYear() != null)
                .max(Comparator.comparing(CompanyFinancialStatistics::getYear))
                .orElse(null);
        return new Financials(
                latest == null ? null : latest.getYear(),
                latest == null ? null : latest.getSalesAmount(),
                round(company.getSalesGrowthRate()),
                round(company.getSupportedSalesGrowthRate()),
                round(company.getDebtRatio()),
                round(company.getGovernmentRndDependency()),
                latest == null ? null : latest.getResearchAndDevelopmentExpense());
    }

    private Employment employment(List<CompanyEmploymentStatistics> employmentStatistics) {
        CompanyEmploymentStatistics latest = employmentStatistics.stream()
                .filter(stat -> stat.getYear() != null)
                .max(Comparator.comparing(CompanyEmploymentStatistics::getYear))
                .orElse(null);
        if (latest == null) {
            return new Employment(null, null, null, null, null, null, null);
        }
        Integer previousEmployeeCount = employmentStatistics.stream()
                .filter(stat -> Objects.equals(stat.getYear(), latest.getYear() - 1))
                .map(CompanyEmploymentStatistics::getEmployeeCount)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return new Employment(
                latest.getYear(),
                previousEmployeeCount,
                latest.getEmployeeCount(),
                latest.getPensionSubscriberCount(),
                latest.getPensionNewHireCount(),
                latest.getPensionRetireeCount(),
                round(calculateTurnoverRate(previousEmployeeCount, latest)));
    }

    private SupportHistory supportHistory(List<BtpSupportHistory> supportHistories, List<BtpSupportHistory> selectedHistories) {
        List<String> recentSupportTexts = supportHistories.stream()
                .sorted(Comparator.comparing(BtpSupportHistory::getSupportYear, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BtpSupportHistory::getSelectedDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BtpSupportHistory::getSupportHistoryId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::supportText)
                .filter(this::hasText)
                .limit(10)
                .toList();
        return new SupportHistory(
                supportHistories.size(),
                countByKeywords(selectedHistories, MARKET_KEYWORDS),
                countByKeywords(selectedHistories, TECH_RND_KEYWORDS),
                selectedHistories.stream().anyMatch(history -> containsAny(supportText(history), JOB_CREATION_KEYWORDS)),
                recentSupportTexts);
    }

    private PatentSummary patentSummary(Integer companyId) {
        long activeCount = patentRepository.countByCompanyIdAndRegistrationStatusAndIsActiveTrue(
                companyId, PatentRegistrationStatuses.REGISTERED);
        Integer latestRegistrationYear = patentRepository.findByCompanyIdOrderByRegistrationDateDescPatentIdAsc(companyId)
                .stream()
                .map(CompanyPatent::getRegistrationDate)
                .filter(Objects::nonNull)
                .map(LocalDate::getYear)
                .findFirst()
                .orElse(null);
        return new PatentSummary(activeCount, latestRegistrationYear);
    }

    private Double calculateTurnoverRate(Integer previousEmployeeCount, CompanyEmploymentStatistics latest) {
        Double averageWorkforce = averageWorkforce(previousEmployeeCount, latest);
        if (averageWorkforce == null || averageWorkforce <= 0 || latest.getPensionRetireeCount() == null) {
            return null;
        }
        return latest.getPensionRetireeCount() * 100.0 / averageWorkforce;
    }

    private Double averageWorkforce(Integer previousEmployeeCount, CompanyEmploymentStatistics latest) {
        if (previousEmployeeCount != null && latest.getEmployeeCount() != null) {
            return (previousEmployeeCount + latest.getEmployeeCount()) / 2.0;
        }
        if (latest.getPensionSubscriberCount() != null) {
            return latest.getPensionSubscriberCount().doubleValue();
        }
        return latest.getEmployeeCount() == null ? null : latest.getEmployeeCount().doubleValue();
    }

    private int countByKeywords(List<BtpSupportHistory> histories, List<String> keywords) {
        return (int) histories.stream()
                .filter(history -> containsAny(supportText(history), keywords))
                .count();
    }

    private String supportText(BtpSupportHistory history) {
        List<String> parts = new ArrayList<>();
        if (history.getSupportYear() != null) {
            parts.add(String.valueOf(history.getSupportYear()));
        }
        parts.addAll(List.of(
                nullToEmpty(history.getBudgetProgramName()),
                nullToEmpty(history.getSupportType()),
                nullToEmpty(history.getSupportCategory()),
                nullToEmpty(history.getSupportDetail()),
                nullToEmpty(history.getSupportItem()),
                nullToEmpty(history.getSelectionResult())));
        return joinText(parts.toArray(String[]::new));
    }

    private boolean isSelected(BtpSupportHistory history) {
        return SELECTED_RESULTS.contains(history.getSelectionResult());
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (!hasText(text)) {
            return false;
        }
        String lower = text.toLowerCase();
        return keywords.stream().anyMatch(keyword -> lower.contains(keyword.toLowerCase()));
    }

    private String joinText(String... values) {
        return Stream.of(values)
                .filter(this::hasText)
                .reduce("", (left, right) -> hasText(left) ? left + " " + right.trim() : right.trim());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Double round(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
