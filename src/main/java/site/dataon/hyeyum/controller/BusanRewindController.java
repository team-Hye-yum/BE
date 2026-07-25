package site.dataon.hyeyum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.BusanRewindResponses.CurrentStatus;
import site.dataon.hyeyum.dto.BusanRewindResponses.CurrentSupportPrograms;
import site.dataon.hyeyum.dto.BusanRewindResponses.PastSupportReview;
import site.dataon.hyeyum.dto.BusanRewindResponses.SimilarFlow;
import site.dataon.hyeyum.dto.BusanRewindResponses.SupportComparison;
import site.dataon.hyeyum.dto.BusanRewindResponses.TrendBriefing;
import site.dataon.hyeyum.service.BusanRewindService;

@Validated
@RestController
@RequestMapping("/busan-rewind")
@Tag(name = "Busan Rewind", description = "Past support program based industry review APIs")
public class BusanRewindController {

    private final BusanRewindService busanRewindService;

    public BusanRewindController(BusanRewindService busanRewindService) {
        this.busanRewindService = busanRewindService;
    }

    @GetMapping("/current-status")
    @Operation(summary = "Get current industry status")
    public ApiDataResponse<CurrentStatus> currentStatus(
            @Parameter(description = "KSIC division code", example = "28")
                    @RequestParam("industryCode")
                    @NotBlank
                    String industryCode) {
        return busanRewindService.currentStatus(industryCode);
    }

    @GetMapping("/current-support-programs")
    @Operation(summary = "Get current BTP support programs")
    public ApiDataResponse<CurrentSupportPrograms> currentSupportPrograms(
            @Parameter(description = "KSIC division code", example = "28")
                    @RequestParam("industryCode")
                    @NotBlank
                    String industryCode) {
        return busanRewindService.currentSupportPrograms(industryCode);
    }

    @GetMapping("/trend-briefing")
    @Operation(summary = "Get industry trend briefing")
    public ApiDataResponse<TrendBriefing> trendBriefing(
            @Parameter(description = "KSIC division code", example = "28")
                    @RequestParam("industryCode")
                    @NotBlank
                    String industryCode) {
        return busanRewindService.trendBriefing(industryCode);
    }

    @GetMapping("/similar-flow")
    @Operation(summary = "Get similar historical industry flow")
    public ApiDataResponse<SimilarFlow> similarFlow(
            @Parameter(description = "KSIC division code", example = "28")
                    @RequestParam("industryCode")
                    @NotBlank
                    String industryCode) {
        return busanRewindService.similarFlow(industryCode);
    }

    @GetMapping("/past-support-review")
    @Operation(summary = "Get past industry and support program review")
    public ApiDataResponse<PastSupportReview> pastSupportReview(
            @Parameter(description = "KSIC division code", example = "28")
                    @RequestParam("industryCode")
                    @NotBlank
                    String industryCode) {
        return busanRewindService.pastSupportReview(industryCode);
    }

    @GetMapping("/support-comparison")
    @Operation(summary = "Compare past and current support programs")
    public ApiDataResponse<SupportComparison> supportComparison(
            @Parameter(description = "KSIC division code", example = "28")
                    @RequestParam("industryCode")
                    @NotBlank
                    String industryCode) {
        return busanRewindService.supportComparison(industryCode);
    }
}
