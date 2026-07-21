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
import site.dataon.hyeyum.dto.CompanyScorecardResponses.BusanAxDxEvidenceResponse;
import site.dataon.hyeyum.dto.CompanyScorecardResponses.CompanyScorecardSummaryResponse;
import site.dataon.hyeyum.service.BusanAxDxEvidenceService;
import site.dataon.hyeyum.service.CompanyScorecardService;

@Validated
@RestController
@RequestMapping("/companies/{companyId}/scorecard")
@Tag(name = "Company Scorecard", description = "기업 종합 스코어카드 API")
public class CompanyScorecardController {

    private final CompanyScorecardService companyScorecardService;
    private final BusanAxDxEvidenceService busanAxDxEvidenceService;

    public CompanyScorecardController(
            CompanyScorecardService companyScorecardService,
            BusanAxDxEvidenceService busanAxDxEvidenceService) {
        this.companyScorecardService = companyScorecardService;
        this.busanAxDxEvidenceService = busanAxDxEvidenceService;
    }

    @GetMapping("/summary")
    @Operation(summary = "기업 종합 스코어카드", description = "재무, 고용, 특허·인증, 연구·활동 요약 카드를 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "기업 종합 스코어카드 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<CompanyScorecardSummaryResponse> summary(
            @Parameter(description = "기업일련번호", example = "117")
                    @PathVariable("companyId")
                    @NotNull
                    Integer companyId) {
        return companyScorecardService.summary(companyId);
    }

    @GetMapping("/busan-axdx-evidence")
    @Operation(summary = "부산 AX/DX 근거", description = "부산 AX/DX 뱃지 표시 근거를 실행 이력, 연관 산업·기술 순서로 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "부산 AX/DX 근거 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<BusanAxDxEvidenceResponse> busanAxDxEvidence(
            @Parameter(description = "기업일련번호", example = "1178")
                    @PathVariable("companyId")
                    @NotNull
                    Integer companyId) {
        return busanAxDxEvidenceService.findEvidence(companyId);
    }
}
