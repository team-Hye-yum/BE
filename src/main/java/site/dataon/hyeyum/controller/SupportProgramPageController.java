package site.dataon.hyeyum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Support Programs", description = "사업별 목록화 페이지 API")
public class SupportProgramPageController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SupportProgramPageService supportProgramPageService;

    public SupportProgramPageController(SupportProgramPageService supportProgramPageService) {
        this.supportProgramPageService = supportProgramPageService;
    }

    @GetMapping("/search")
    @Operation(summary = "지원 사업 키워드 검색", description = "검색창 key event마다 호출해 키워드가 포함된 지원 사업 목록을 반환한다.")
    public ApiDataResponse<SupportProgramSearchResponse> search(
            @Parameter(description = "지원 사업명 검색어", example = "성장 사다리") @RequestParam("keyword") @NotBlank String keyword) {
        return supportProgramPageService.search(keyword);
    }

    @GetMapping("/{supportProgramCode}/companies")
    @Operation(summary = "사업별 신청 기업 목록", description = "선택한 지원 사업에 신청한 기업들의 고정 컬럼 목록을 반환한다.")
    public ApiDataResponse<SupportProgramCompanyListResponse> companies(
            @Parameter(description = "지원 사업 코드", example = "B1_311")
                    @PathVariable("supportProgramCode")
                    @NotBlank
                    String supportProgramCode) {
        return supportProgramPageService.companies(supportProgramCode);
    }

    @GetMapping("/{supportProgramCode}/companies/excel")
    @Operation(summary = "사업별 신청 기업 목록 Excel 다운로드", description = "화면에 표시되는 고정 컬럼 전체를 Excel 파일로 다운로드한다.")
    public ResponseEntity<byte[]> companyListExcel(
            @Parameter(description = "지원 사업 코드", example = "B1_311")
                    @PathVariable("supportProgramCode")
                    @NotBlank
                    String supportProgramCode) {
        return excelResponse(
                supportProgramPageService.companyListExcel(supportProgramCode),
                "support-program-companies-" + supportProgramCode + ".xlsx");
    }

    @GetMapping("/company-template")
    @Operation(summary = "기업정보 Excel 템플릿 다운로드", description = "기업정보 업로드에 사용하는 Excel 템플릿 파일을 다운로드한다.")
    public ResponseEntity<byte[]> companyTemplate() {
        return excelResponse(supportProgramPageService.companyTemplate(), "company-info-template.xlsx");
    }

    @PostMapping(value = "/announcement-analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "BTP 지원 사업 공고 PDF 분석", description = "업로드한 공고 PDF를 분석해 지원 사업 저장 후보 값을 반환한다.")
    public ApiDataResponse<SupportProgramAnnouncementAnalysisResponse> analyzeAnnouncement(
            @Parameter(description = "BTP 지원 사업 공고 PDF")
                    @RequestPart("announcementPdf")
                    @NotNull
                    MultipartFile announcementPdf) {
        return supportProgramPageService.analyzeAnnouncement(announcementPdf);
    }

    @PostMapping
    @Operation(summary = "BTP 지원 사업 공고 업로드", description = "담당자가 수정한 지원 사업 분석 결과를 저장하거나 갱신한다.")
    public ApiDataResponse<SupportProgramSaveResponse> save(@RequestBody @Valid SupportProgramSaveRequest request) {
        return supportProgramPageService.save(request);
    }

    @PostMapping(value = "/company-template/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "기업정보 Excel 템플릿 업로드", description = "담당자가 작성한 기업정보 Excel 템플릿을 업로드한다.")
    public ApiDataResponse<CompanyTemplateImportResponse> importCompanyTemplate(
            @Parameter(description = "기업정보 템플릿 Excel 파일") @RequestPart("file") @NotNull MultipartFile file) {
        return supportProgramPageService.importCompanyTemplate(file);
    }

    private ResponseEntity<byte[]> excelResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
