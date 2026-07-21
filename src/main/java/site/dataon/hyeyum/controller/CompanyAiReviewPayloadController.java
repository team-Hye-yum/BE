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
import site.dataon.hyeyum.dto.CompanyAiReviewPayloadResponse;
import site.dataon.hyeyum.service.CompanyAiReviewPayloadService;

@Validated
@RestController
@RequestMapping("/companies/{companyId}/ai-review")
@Tag(name = "Company AI Review", description = "Company AI review payload API")
public class CompanyAiReviewPayloadController {

    private final CompanyAiReviewPayloadService payloadService;

    public CompanyAiReviewPayloadController(CompanyAiReviewPayloadService payloadService) {
        this.payloadService = payloadService;
    }

    @GetMapping("/payload")
    @Operation(summary = "Build FastAPI review payload")
    public ApiDataResponse<CompanyAiReviewPayloadResponse> payload(
            @PathVariable("companyId") @NotNull Integer companyId) {
        return payloadService.payload(companyId);
    }
}
