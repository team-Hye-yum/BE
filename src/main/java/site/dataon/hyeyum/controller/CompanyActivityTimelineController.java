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
import site.dataon.hyeyum.dto.CompanyActivitySupportTimelineResponse;
import site.dataon.hyeyum.service.CompanyActivityTimelineService;

@Validated
@RestController
@RequestMapping("/companies/{companyId}")
@Tag(name = "Company Activity Timeline", description = "기업 활동·지원 이력 타임라인 API")
public class CompanyActivityTimelineController {

    private final CompanyActivityTimelineService companyActivityTimelineService;

    public CompanyActivityTimelineController(CompanyActivityTimelineService companyActivityTimelineService) {
        this.companyActivityTimelineService = companyActivityTimelineService;
    }

    @GetMapping("/activity-support-timeline")
    @Operation(
            summary = "기업 활동·지원 이력 타임라인",
            description = "특허, 국가 R&D, 부산TP 지원 이력을 하나의 시간축에 표시할 수 있도록 사건 목록을 반환한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "기업 활동·지원 이력 조회 성공"),
        @ApiResponse(responseCode = "404", description = "기업 정보를 찾을 수 없음")
    })
    public ApiDataResponse<CompanyActivitySupportTimelineResponse> activitySupportTimeline(
            @Parameter(description = "기업 일련번호", example = "1786")
                    @PathVariable("companyId")
                    @NotNull
                    Integer companyId) {
        return companyActivityTimelineService.activitySupportTimeline(companyId);
    }
}
