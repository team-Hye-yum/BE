package site.dataon.hyeyum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisCollaborativeProjectListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.NtisLeadProjectListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.PatentListResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ProductiveActivitiesSummaryResponse;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.ResearchDevelopmentStatusResponse;
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
    @Operation(summary = "기업 정보", description = "기업일련번호(companyId)를 기준으로 기업 기본 정보와 업력 계산값을 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "기업 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<CompanyProfileResponse> profile(
            @Parameter(description = "기업일련번호", example = "117")
                    @PathVariable("companyId")
                    @NotNull
                    Integer companyId) {
        return companyDashboardService.profile(companyId);
    }

    @GetMapping("/financial-position")
    @Operation(summary = "재무 주요 지표", description = "연도별 자산, 부채, 자본, 납입자본금과 부채비율을 천원 단위로 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "재무 주요 지표 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<FinancialPositionResponse> financialPosition(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.financialPosition(companyId);
    }

    @GetMapping("/income-statements")
    @Operation(summary = "손익 주요 지표", description = "연도별 매출, 매출원가, 영업이익, 당기순이익, 영업이익률과 전년 대비 매출 증가율을 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "손익 주요 지표 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<IncomeStatementsResponse> incomeStatements(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.incomeStatements(companyId);
    }

    @GetMapping("/employments")
    @Operation(summary = "고용 정보", description = "연도별 종업원 수, 국민연금 가입/취업/퇴직자 수, 평균 급여와 고용 증감률을 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "고용 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<EmploymentsResponse> employments(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.employments(companyId);
    }

    @GetMapping("/certifications-ip-summary")
    @Operation(summary = "인증·지식재산권 요약", description = "활성 등록 특허 수, 인증 배지, 연구조직 보유 여부와 등록일을 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "인증·지식재산권 요약 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<CertificationsIpSummaryResponse> certificationsIpSummary(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.certificationsIpSummary(companyId);
    }

    @GetMapping("/research-development/status")
    @Operation(summary = "연구개발 조직·인력 현황", description = "최근 연구원수, 기업부설연구소 보유 여부와 등록일, 연구개발전담부서 보유 여부와 등록일을 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "연구개발 조직·인력 현황 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<ResearchDevelopmentStatusResponse> researchDevelopmentStatus(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.researchDevelopmentStatus(companyId);
    }

    @GetMapping("/patents")
    @Operation(summary = "특허 상세 모달", description = "특허 현황 클릭 시 표시할 기업별 특허/실용신안 상세 목록을 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "특허 상세 목록 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<PatentListResponse> patents(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.patents(companyId);
    }

    @GetMapping("/productive-activities/summary")
    @Operation(summary = "생산적 활동 이력 요약", description = "NTIS 참여 요약, 최근 5개년 정부 연구비 합계, 지원 이력 요약을 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "생산적 활동 이력 요약 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<ProductiveActivitiesSummaryResponse> productiveActivitiesSummary(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.productiveActivitiesSummary(companyId);
    }

    @GetMapping("/ntis/lead-projects")
    @Operation(summary = "NTIS 주관 과제 모달", description = "기업이 주관한 NTIS 과제 목록과 연구 기간, 연구비 정보를 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "NTIS 주관 과제 목록 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<NtisLeadProjectListResponse> ntisLeadProjects(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.ntisLeadProjects(companyId);
    }

    @GetMapping("/ntis/collaborative-projects")
    @Operation(summary = "NTIS 위탁/공동 과제 모달", description = "기업이 위탁/공동 형태로 참여한 NTIS 과제 목록과 공동연구 정보를 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "NTIS 위탁/공동 과제 목록 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<NtisCollaborativeProjectListResponse> ntisCollaborativeProjects(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.ntisCollaborativeProjects(companyId);
    }

    @GetMapping("/computed-metrics")
    @Operation(summary = "계산 지표", description = "부채비율, 매출원가율, 매출/고용 성장성, 정부 R&D 의존도 등 저장된 계산 지표를 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "계산 지표 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<ComputedMetricsResponse> computedMetrics(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.computedMetrics(companyId);
    }

    @GetMapping("/growth-scenario")
    @Operation(
            summary = "기업 성장 시나리오",
            description = "2021년 기준 기업 매출 성장지수와 한국은행 동일 업종 성장지수를 반환한다. 2025년 기업 참고 경로는 업종 성장률에 최근 2개년 업종 대비 초과/부족 성장 중앙값을 더해 산출한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "기업 성장 시나리오 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<GrowthScenarioResponse> growthScenario(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.growthScenario(companyId);
    }

    @GetMapping("/ai-summary")
    @Operation(summary = "AI 3줄 요약", description = "company.ai_summary에 저장된 기업 3줄 요약을 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "AI 3줄 요약 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<AiSummaryResponse> aiSummary(@PathVariable("companyId") @NotNull Integer companyId) {
        return companyDashboardService.aiSummary(companyId);
    }
}
