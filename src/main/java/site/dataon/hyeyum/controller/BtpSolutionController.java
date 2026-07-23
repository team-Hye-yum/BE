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
import site.dataon.hyeyum.dto.BtpSolutionConnectionEvidenceCompaniesResponse;
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

    @GetMapping("/industries/{divisionCode}/overview")
    @Operation(
            summary = "Get BTP solution industry overview",
            description = "Returns Busan public statistics and BTP-supported company ratios for a KSIC division.")
    public ApiDataResponse<BtpSolutionIndustryOverviewResponse> overview(
            @Parameter(description = "KSIC division code", example = "29")
                    @PathVariable("divisionCode")
                    @NotBlank
                    String divisionCode) {
        return industryService.overview(divisionCode);
    }

    @GetMapping("/industries/{divisionCode}/infra-hubs")
    @Operation(
            summary = "Get BTP infrastructure hubs",
            description =
                    "Returns official BTP hubs with equipment counts, top equipment categories, sample equipment, and facility details.")
    public ApiDataResponse<BtpSolutionInfraHubResponse> infraHubs(
            @Parameter(description = "KSIC division code", example = "29")
                    @PathVariable("divisionCode")
                    @NotBlank
                    String divisionCode) {
        return industryService.infraHubs(divisionCode);
    }

    @GetMapping("/industries/{divisionCode}/connection-evidence/companies")
    @Operation(
            summary = "Get BTP solution company connection evidence",
            description =
                    "Returns company-level evidence that connects selected industry companies to equipment and BTP hubs.")
    public ApiDataResponse<BtpSolutionConnectionEvidenceCompaniesResponse> connectionEvidenceCompanies(
            @Parameter(description = "KSIC division code", example = "29")
                    @PathVariable("divisionCode")
                    @NotBlank
                    String divisionCode,
            @Parameter(description = "Company name keyword", example = "조선")
                    @RequestParam(value = "keyword", defaultValue = "")
                    String keyword,
            @Parameter(description = "Hub id filter", example = "1")
                    @RequestParam(value = "hubId", required = false)
                    Long hubId,
            @Parameter(description = "Page number starting from 0", example = "0")
                    @RequestParam(value = "page", defaultValue = "0")
                    @Min(0)
                    int page,
            @Parameter(description = "Page size", example = "10")
                    @RequestParam(value = "size", defaultValue = "10")
                    @Min(1)
                    int size) {
        return industryService.connectionEvidenceCompanies(divisionCode, keyword, hubId, page, size);
    }
}
