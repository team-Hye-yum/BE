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
import site.dataon.hyeyum.dto.SupportHistoryCurrentYearStatusResponse;
import site.dataon.hyeyum.dto.SupportHistoryLatestVsPastResponse;
import site.dataon.hyeyum.dto.SupportHistoryPostSupportChangeResponse;
import site.dataon.hyeyum.service.CompanySupportHistoryReviewService;

@Validated
@RestController
@RequestMapping("/companies/{companyId}/support-history")
@Tag(name = "Company Support History", description = "기업 지원 이력 통합 검토 API")
public class CompanySupportHistoryController {

    private final CompanySupportHistoryReviewService supportHistoryReviewService;

    public CompanySupportHistoryController(CompanySupportHistoryReviewService supportHistoryReviewService) {
        this.supportHistoryReviewService = supportHistoryReviewService;
    }

    @GetMapping("/review/latest-vs-past")
    @Operation(
            summary = "최신 지원 이력-과거 지원 이력 비교",
            description = "기업의 최신 지원연도 이력과 과거 부산TP 지원 이력을 비교하여 검토 신호를 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "지원 이력 비교 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<SupportHistoryLatestVsPastResponse> latestVsPast(
            @Parameter(description = "기업 일련번호", example = "1786")
                    @PathVariable("companyId")
                    @NotNull
                    Integer companyId) {
        return supportHistoryReviewService.latestVsPast(companyId);
    }

    @GetMapping("/current-year-status")
    @Operation(
            summary = "금년도 지원 현황",
            description = "기업의 금년도 부산TP 지원 신청/선정 이력을 목록으로 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "금년도 지원 현황 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<SupportHistoryCurrentYearStatusResponse> currentYearStatus(
            @Parameter(description = "기업 일련번호", example = "1786")
                    @PathVariable("companyId")
                    @NotNull
                    Integer companyId) {
        return supportHistoryReviewService.currentYearStatus(companyId);
    }

    @GetMapping("/review/post-support-changes")
    @Operation(
            summary = "지원 이후 변화 확인",
            description = "기업의 부산TP 지원 종료연도별로 다음 해 재무, 고용, 특허 데이터를 요약해 관찰 상태와 함께 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "지원 이후 변화 확인 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<SupportHistoryPostSupportChangeResponse> postSupportChanges(
            @Parameter(description = "기업 일련번호", example = "1786")
                    @PathVariable("companyId")
                    @NotNull
                    Integer companyId) {
        return supportHistoryReviewService.postSupportChanges(companyId);
    }
}
