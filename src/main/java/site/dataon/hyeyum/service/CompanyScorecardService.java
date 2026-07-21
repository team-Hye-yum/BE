package site.dataon.hyeyum.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import site.dataon.hyeyum.common.PatentRegistrationStatuses;
import site.dataon.hyeyum.domain.Company;
import site.dataon.hyeyum.domain.CompanyEmploymentStatistics;
import site.dataon.hyeyum.domain.CompanyFinancialStatistics;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.BadgeItem;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.BusanAxDxBadge;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.BusanAxDxEvidenceResponse;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.CertificationsIpCard;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.CompanyScorecardSummaryResponse;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.DisplayMoneyValue;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.DisplayNumericValue;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.EmploymentCard;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.FinancialCard;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.ResearchActivityCard;
import site.dataon.hyeyum.repository.CompanyEmploymentStatisticsRepository;
import site.dataon.hyeyum.repository.CompanyFinancialStatisticsRepository;
import site.dataon.hyeyum.repository.CompanyNtisLeadProjectRepository;
import site.dataon.hyeyum.repository.CompanyPatentRepository;
import site.dataon.hyeyum.repository.CompanyRepository;

@Service
public class CompanyScorecardService {

    private static final String PATENT_RIGHT = "특허권";

    private final CompanyRepository companyRepository;
    private final CompanyFinancialStatisticsRepository financialStatisticsRepository;
    private final CompanyEmploymentStatisticsRepository employmentStatisticsRepository;
    private final CompanyPatentRepository patentRepository;
    private final CompanyNtisLeadProjectRepository ntisLeadProjectRepository;
    private final BusanAxDxEvidenceService busanAxDxEvidenceService;

    public CompanyScorecardService(
            CompanyRepository companyRepository,
            CompanyFinancialStatisticsRepository financialStatisticsRepository,
            CompanyEmploymentStatisticsRepository employmentStatisticsRepository,
            CompanyPatentRepository patentRepository,
            CompanyNtisLeadProjectRepository ntisLeadProjectRepository,
            BusanAxDxEvidenceService busanAxDxEvidenceService) {
        this.companyRepository = companyRepository;
        this.financialStatisticsRepository = financialStatisticsRepository;
        this.employmentStatisticsRepository = employmentStatisticsRepository;
        this.patentRepository = patentRepository;
        this.ntisLeadProjectRepository = ntisLeadProjectRepository;
        this.busanAxDxEvidenceService = busanAxDxEvidenceService;
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<CompanyScorecardSummaryResponse> summary(Integer companyId) {
        Company company = findCompany(companyId);
        BusanAxDxEvidenceResponse axDxEvidence = busanAxDxEvidenceService.evidence(companyId);
        return new ApiDataResponse<>(new CompanyScorecardSummaryResponse(
                financial(companyId),
                employment(companyId),
                certificationsIp(companyId, company),
                researchActivity(companyId, axDxEvidence)));
    }

    private FinancialCard financial() {
        return new FinancialCard(
                new DisplayMoneyValue(null, "데이터 없음"),
                null,
                new DisplayNumericValue(null, "데이터 없음"),
                new DisplayNumericValue(null, "데이터 없음"));
    }

    private EmploymentCard employment() {
        return new EmploymentCard(null, null, null, new DisplayNumericValue(null, "데이터 없음"));
    }

    private FinancialCard financial(Integer companyId) {
        List<CompanyFinancialStatistics> statistics =
                financialStatisticsRepository.findTop2ByCompanyIdOrderByYearDesc(companyId);
        CompanyFinancialStatistics current = statistics.isEmpty() ? null : statistics.get(0);
        CompanyFinancialStatistics previous = statistics.size() < 2 ? null : statistics.get(1);
        if (current == null) {
            return financial();
        }
        Double growthRate = growthPercent(
                previous == null ? null : previous.getSalesAmount(),
                current.getSalesAmount());
        return new FinancialCard(
                new DisplayMoneyValue(current.getSalesAmount(), displaySalesAmount(current.getSalesAmount())),
                current.getYear(),
                new DisplayNumericValue(growthRate, displayGrowthRate(growthRate)),
                new DisplayNumericValue(round(current.getOperatingMargin()), displayOperatingMargin(current.getOperatingMargin())));
    }

    private EmploymentCard employment(Integer companyId) {
        List<CompanyEmploymentStatistics> statistics =
                employmentStatisticsRepository.findTop2ByCompanyIdOrderByYearDesc(companyId);
        CompanyEmploymentStatistics current = statistics.isEmpty() ? null : statistics.get(0);
        CompanyEmploymentStatistics previous = statistics.size() < 2 ? null : statistics.get(1);
        if (current == null) {
            return employment();
        }
        Integer change = current.getEmployeeCount() == null || previous == null || previous.getEmployeeCount() == null
                ? null
                : current.getEmployeeCount() - previous.getEmployeeCount();
        Double growthRate = growthPercent(
                previous == null ? null : previous.getEmployeeCount(),
                current.getEmployeeCount());
        return new EmploymentCard(
                current.getEmployeeCount(),
                current.getYear(),
                change,
                new DisplayNumericValue(growthRate, displayEmployeeGrowthRate(growthRate, change)));
    }

    private CertificationsIpCard certificationsIp(Integer companyId, Company company) {
        long activePatentCount = patentRepository.countByCompanyIdAndPatentTypeAndRegistrationStatusAndIsActiveTrue(
                companyId, PATENT_RIGHT, PatentRegistrationStatuses.REGISTERED);
        return new CertificationsIpCard(activePatentCount, certificationBadges(company));
    }

    private ResearchActivityCard researchActivity(Integer companyId, BusanAxDxEvidenceResponse axDxEvidence) {
        int ntisLeadProjectCount = ntisLeadProjectRepository.countDistinctLeadProjectsForScorecard(companyId);
        return new ResearchActivityCard(
                ntisLeadProjectCount,
                ntisLeadProjectCount == 0 ? "국가 R&D 수행 없음" : "국가 R&D 수행 " + ntisLeadProjectCount + "건",
                new BusanAxDxBadge(
                        axDxEvidence.badgeVisible(),
                        axDxEvidence.evidenceCount(),
                        axDxEvidence.badgeVisible() ? "관련 근거 " + axDxEvidence.evidenceCount() + "건" : "관련 근거가 확인되지 않음"));
    }

    private List<BadgeItem> certificationBadges(Company company) {
        return java.util.stream.Stream.of(
                        badge("VENTURE", "벤처기업", company.getIsVentureCompany()),
                        badge("INNOBIZ", "이노비즈", company.getIsInnobiz()),
                        badge("MAINBIZ", "메인비즈", company.getIsMainbiz()),
                        badge("MATERIALS_PARTS", "소재부품", company.getIsMaterialsCompany()),
                        badge("NET", "NET", company.getIsNetCertified()),
                        badge("NEP", "NEP", company.getIsNepCertified()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }

    private java.util.Optional<BadgeItem> badge(String code, String label, Boolean active) {
        return Boolean.TRUE.equals(active) ? java.util.Optional.of(new BadgeItem(code, label)) : java.util.Optional.empty();
    }

    private Company findCompany(Integer companyId) {
        return companyRepository
                .findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기업 정보를 찾을 수 없습니다."));
    }

    private String displaySalesAmount(Integer salesAmount) {
        if (salesAmount == null) {
            return "데이터 없음";
        }
        return formatOneDecimal(salesAmount / 100000.0) + "억원";
    }

    private String displayGrowthRate(Double growthRate) {
        if (growthRate == null) {
            return "전년 대비 데이터 없음";
        }
        if (growthRate == 0.0) {
            return "전년 대비 변동 없음";
        }
        return "전년 대비 " + formatOneDecimal(growthRate) + "%";
    }

    private String displayEmployeeGrowthRate(Double growthRate, Integer change) {
        if (growthRate == null) {
            return "전년 대비 데이터 없음";
        }
        if (change != null && change == 0) {
            return "전년 대비 변동 없음";
        }
        return displayGrowthRate(growthRate);
    }

    private String displayOperatingMargin(Double operatingMargin) {
        if (operatingMargin == null) {
            return "영업이익률 데이터 없음";
        }
        return "영업이익률 " + formatOneDecimal(operatingMargin) + "%";
    }

    private Double growthPercent(Integer previous, Integer current) {
        if (previous == null || current == null || previous == 0) {
            return null;
        }
        return round((current - previous) * 100.0 / previous);
    }

    private Double round(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatOneDecimal(Double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
