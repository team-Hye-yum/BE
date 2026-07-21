package site.dataon.hyeyum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyAiAnalysisPayloadResponse;
import site.dataon.hyeyum.service.CompanyAiAnalysisPayloadService;

@Validated
@RestController
@RequestMapping("/companies/{companyId}/ai-analysis")
@Tag(name = "Company AI Analysis", description = "Company AI analysis payload API")
public class CompanyAiAnalysisPayloadController {

    private final CompanyAiAnalysisPayloadService payloadService;

    public CompanyAiAnalysisPayloadController(CompanyAiAnalysisPayloadService payloadService) {
        this.payloadService = payloadService;
    }

    @GetMapping("/payload")
    @Operation(summary = "Build FastAPI AI analysis payload")
    public ApiDataResponse<CompanyAiAnalysisPayloadResponse> payload(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return payloadService.payload(companyId);
    }
}
