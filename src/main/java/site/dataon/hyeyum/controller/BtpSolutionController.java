package site.dataon.hyeyum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.BtpSolutionInfraHubResponse;
import site.dataon.hyeyum.dto.BtpSolutionIndustryOverviewResponse;
import site.dataon.hyeyum.dto.KsicIndustrySearchResponse;
import site.dataon.hyeyum.service.BtpSolutionIndustryService;

@Validated
@RestController
@RequestMapping("/btp-solution")
@Tag(name = "BTP Solution", description = "BTP solution search and analysis APIs")
public class BtpSolutionController {

    private final BtpSolutionIndustryService industryService;

    public BtpSolutionController(BtpSolutionIndustryService industryService) {
        this.industryService = industryService;
    }

    @GetMapping("/industries/search")
    @Operation(
            summary = "Search KSIC industries",
            description = "Searches KSIC hierarchy names and returns selectable industries with their section code.")
    public ApiDataResponse<KsicIndustrySearchResponse> searchIndustries(
            @Parameter(description = "KSIC code or industry keyword", example = "조선")
                    @RequestParam("keyword")
                    @NotBlank
                    String keyword,
            @Parameter(description = "Maximum result count", example = "10")
                    @RequestParam(value = "limit", defaultValue = "10")
                    @Min(1)
                    int limit) {
        return industryService.searchIndustries(keyword, limit);
    }

    @GetMapping("/industries/{sectionCode}/overview")
    @Operation(
            summary = "Get BTP solution industry overview",
            description = "Returns Busan public statistics and BTP-supported company ratios for a KSIC section.")
    public ApiDataResponse<BtpSolutionIndustryOverviewResponse> overview(
            @Parameter(description = "KSIC section code", example = "C")
                    @PathVariable("sectionCode")
                    @NotBlank
                    String sectionCode) {
        return industryService.overview(sectionCode);
    }

    @GetMapping("/industries/{sectionCode}/infra-hubs")
    @Operation(
            summary = "Get BTP infrastructure hubs",
            description =
                    "Returns official BTP hubs with equipment counts, top equipment categories, sample equipment, and facility details.")
    public ApiDataResponse<BtpSolutionInfraHubResponse> infraHubs(
            @Parameter(description = "KSIC section code", example = "C")
                    @PathVariable("sectionCode")
                    @NotBlank
                    String sectionCode) {
        return industryService.infraHubs(sectionCode);
    }
}
