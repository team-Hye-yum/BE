package site.dataon.hyeyum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.AiSummaryResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.CertificationsIpSummaryResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.CompanyProfileResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ComputedMetricsResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.EmploymentsResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.FinancialPositionResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.GrowthScenarioResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.IncomeStatementsResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NewsResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisCollaborativeProjectListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisLeadProjectListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.PatentListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ProductiveActivitiesSummaryResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.SupportDuplicationReviewResponse;
import site.dataon.hyeyum.service.CompanyDashboardService;

@Validated
@RestController
@RequestMapping("/companies/{companyId}")
@Tag(name = "Company Dashboard", description = "기업 상세 대시보드 API")
public class CompanyDashboardController {

    private final CompanyDashboardService companyDashboardService;

    public CompanyDashboardController(CompanyDashboardService companyDashboardService) {
        this.companyDashboardService = companyDashboardService;
    }

    @GetMapping("/profile")
    @Operation(summary = "기업 정보")
    public ApiDataResponse<CompanyProfileResponse> profile(
            @Parameter(description = "기업일련번호", example = "117")
                    @PathVariable("companyId")
                    @NotNull
                    Integer companyId) {
        return companyDashboardService.profile(companyId);
    }

    @GetMapping("/financial-position")
    @Operation(summary = "재무 주요 지표")
    public ApiDataResponse<FinancialPositionResponse> financialPosition(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.financialPosition(companyId);
    }

    @GetMapping("/income-statements")
    @Operation(summary = "손익 주요 지표")
    public ApiDataResponse<IncomeStatementsResponse> incomeStatements(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.incomeStatements(companyId);
    }

    @GetMapping("/employments")
    @Operation(summary = "고용 정보")
    public ApiDataResponse<EmploymentsResponse> employments(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.employments(companyId);
    }

    @GetMapping("/certifications-ip-summary")
    @Operation(summary = "인증·지식재산권 요약")
    public ApiDataResponse<CertificationsIpSummaryResponse> certificationsIpSummary(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.certificationsIpSummary(companyId);
    }

    @GetMapping("/patents")
    @Operation(summary = "특허 상세 모달")
    public ApiDataResponse<PatentListResponse> patents(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.patents(companyId);
    }

    @GetMapping("/productive-activities/summary")
    @Operation(summary = "생산적 활동 이력 요약")
    public ApiDataResponse<ProductiveActivitiesSummaryResponse> productiveActivitiesSummary(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.productiveActivitiesSummary(companyId);
    }

    @GetMapping("/ntis/lead-projects")
    @Operation(summary = "NTIS 주관 과제 모달")
    public ApiDataResponse<NtisLeadProjectListResponse> ntisLeadProjects(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.ntisLeadProjects(companyId);
    }

    @GetMapping("/ntis/collaborative-projects")
    @Operation(summary = "NTIS 위탁/공동 과제 모달")
    public ApiDataResponse<NtisCollaborativeProjectListResponse> ntisCollaborativeProjects(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.ntisCollaborativeProjects(companyId);
    }

    @GetMapping("/computed-metrics")
    @Operation(summary = "계산 지표")
    public ApiDataResponse<ComputedMetricsResponse> computedMetrics(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.computedMetrics(companyId);
    }

    @GetMapping("/growth-scenario")
    @Operation(summary = "기업 성장 시나리오")
    public ApiDataResponse<GrowthScenarioResponse> growthScenario(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.growthScenario(companyId);
    }

    @GetMapping("/support-duplication-review")
    @Operation(summary = "중복지원 검토")
    public ApiDataResponse<SupportDuplicationReviewResponse> supportDuplicationReview(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.supportDuplicationReview(companyId);
    }

    @GetMapping("/ai-summary")
    @Operation(summary = "AI 3줄 요약")
    public ApiDataResponse<AiSummaryResponse> aiSummary(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.aiSummary(companyId);
    }

    @GetMapping("/news")
    @Operation(summary = "기업별 최신 뉴스")
    public ApiDataResponse<NewsResponse> news(
            @PathVariable("companyId") @NotNull Integer companyId,
            @RequestParam(defaultValue = "3") @Min(1) @Max(10) Integer limit) {
        return companyDashboardService.news(companyId, limit);
    }
}
