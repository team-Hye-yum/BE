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
import site.dataon.hyeyum.dto.BtpSolutionFunctionInfraCoverageResponse;
import site.dataon.hyeyum.dto.BtpSolutionInfraConnectionMatrixResponse;
import site.dataon.hyeyum.dto.BtpSolutionInfraConnectionPositionResponse;
import site.dataon.hyeyum.dto.BtpSolutionInfraHubResponse;
import site.dataon.hyeyum.dto.BtpSolutionIndustryOverviewResponse;
import site.dataon.hyeyum.dto.BtpSolutionRelatedSupportProgramEquipmentsResponse;
import site.dataon.hyeyum.dto.BtpSolutionRelatedSupportProgramsResponse;
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

    @GetMapping("/industries/infra-connection-matrix")
    @Operation(
            summary = "Get industry-infra connection matrix",
            description = "Returns industry matrix points for employee growth rate and function-infra connection rate.")
    public ApiDataResponse<BtpSolutionInfraConnectionMatrixResponse> infraConnectionMatrix(
            @Parameter(description = "Industry granularity: division or group", example = "group")
                    @RequestParam(value = "level", defaultValue = "division")
                    String level) {
        return industryService.infraConnectionMatrix(level);
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

    @GetMapping("/industries/{divisionCode}/function-infra-coverage")
    @Operation(
            summary = "Get function-infra coverage",
            description =
                    "Returns the ratio of detected industry function candidates that have BTP equipment and hub connection evidence.")
    public ApiDataResponse<BtpSolutionFunctionInfraCoverageResponse> functionInfraCoverage(
            @Parameter(description = "KSIC division code", example = "29")
                    @PathVariable("divisionCode")
                    @NotBlank
                    String divisionCode) {
        return industryService.functionInfraCoverage(divisionCode);
    }

    @GetMapping("/industries/{divisionCode}/infra-connection-position")
    @Operation(
            summary = "Get selected industry infra-connection position",
            description = "Returns selected industry's matrix position summary.")
    public ApiDataResponse<BtpSolutionInfraConnectionPositionResponse> infraConnectionPosition(
            @Parameter(description = "KSIC division code", example = "29")
                    @PathVariable("divisionCode")
                    @NotBlank
                    String divisionCode) {
        return industryService.infraConnectionPosition(divisionCode);
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

    @GetMapping("/industries/{divisionCode}/related-support-programs")
    @Operation(
            summary = "Get related support programs",
            description = "Returns BTP support programs related to a selected industry.")
    public ApiDataResponse<BtpSolutionRelatedSupportProgramsResponse> relatedSupportPrograms(
            @Parameter(description = "KSIC division code", example = "29")
                    @PathVariable("divisionCode")
                    @NotBlank
                    String divisionCode) {
        return industryService.relatedSupportPrograms(divisionCode);
    }

    @GetMapping("/industries/{divisionCode}/related-support-programs/{programId}/equipments")
    @Operation(
            summary = "Get related support program equipments",
            description = "Returns paged BTP equipments related to the selected industry for a support program context.")
    public ApiDataResponse<BtpSolutionRelatedSupportProgramEquipmentsResponse> relatedSupportProgramEquipments(
            @Parameter(description = "KSIC division code", example = "29")
                    @PathVariable("divisionCode")
                    @NotBlank
                    String divisionCode,
            @Parameter(description = "Support program id", example = "101")
                    @PathVariable("programId")
                    @Min(1)
                    Long programId,
            @Parameter(description = "Page number starting from 0", example = "0")
                    @RequestParam(value = "page", defaultValue = "0")
                    @Min(0)
                    int page,
            @Parameter(description = "Page size", example = "5")
                    @RequestParam(value = "size", defaultValue = "5")
                    @Min(1)
                    int size) {
        return industryService.relatedSupportProgramEquipments(divisionCode, programId, page, size);
    }
}
