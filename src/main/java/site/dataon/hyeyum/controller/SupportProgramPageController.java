package site.dataon.hyeyum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyTemplateImportResponse;
import site.dataon.hyeyum.dto.SupportProgramAnnouncementAnalysisResponse;
import site.dataon.hyeyum.dto.SupportProgramCompanyListResponse;
import site.dataon.hyeyum.dto.SupportProgramSaveRequest;
import site.dataon.hyeyum.dto.SupportProgramSaveResponse;
import site.dataon.hyeyum.dto.SupportProgramSearchResponse;
import site.dataon.hyeyum.service.SupportProgramPageService;

@Validated
@RestController
@RequestMapping("/support-programs")
public class SupportProgramPageController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SupportProgramPageService supportProgramPageService;

    public SupportProgramPageController(SupportProgramPageService supportProgramPageService) {
        this.supportProgramPageService = supportProgramPageService;
    }

    @GetMapping("/search")
    public ApiDataResponse<SupportProgramSearchResponse> search(@RequestParam("keyword") @NotBlank String keyword) {
        return supportProgramPageService.search(keyword);
    }

    @GetMapping("/{supportProgramId}/companies")
    public ApiDataResponse<SupportProgramCompanyListResponse> companies(
            @PathVariable("supportProgramId") @NotNull Long supportProgramId) {
        return supportProgramPageService.companies(supportProgramId);
    }

    @GetMapping("/{supportProgramId}/companies/excel")
    public ResponseEntity<byte[]> companyListExcel(@PathVariable("supportProgramId") @NotNull Long supportProgramId) {
        return excelResponse(
                supportProgramPageService.companyListExcel(supportProgramId),
                "support-program-companies-" + supportProgramId + ".xlsx");
    }

    @GetMapping("/company-template")
    public ResponseEntity<byte[]> companyTemplate() {
        return excelResponse(supportProgramPageService.companyTemplate(), "company-info-template.xlsx");
    }

    @PostMapping(value = "/announcement-analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiDataResponse<SupportProgramAnnouncementAnalysisResponse> analyzeAnnouncement(
            @RequestPart("announcementPdf") @NotNull MultipartFile announcementPdf) {
        return supportProgramPageService.analyzeAnnouncement(announcementPdf);
    }

    @PostMapping
    public ApiDataResponse<SupportProgramSaveResponse> save(@RequestBody @Valid SupportProgramSaveRequest request) {
        return supportProgramPageService.save(request);
    }

    @PostMapping(value = "/company-template/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiDataResponse<CompanyTemplateImportResponse> importCompanyTemplate(
            @RequestPart("file") @NotNull MultipartFile file) {
        return supportProgramPageService.importCompanyTemplate(file);
    }

    private ResponseEntity<byte[]> excelResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
