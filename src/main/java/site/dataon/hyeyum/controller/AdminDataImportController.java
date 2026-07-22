package site.dataon.hyeyum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.dto.CompanyMetricRecalculationResult;
import site.dataon.hyeyum.dto.DataImportResult;
import site.dataon.hyeyum.dto.DummyDataGenerationResult;
import site.dataon.hyeyum.service.BtpEquipmentTextImportService;
import site.dataon.hyeyum.service.BtpSupportExcelImportService;
import site.dataon.hyeyum.service.CompanyMetricCalculationService;
import site.dataon.hyeyum.service.DummyDataGenerationService;
import site.dataon.hyeyum.service.KodataExcelImportService;
import site.dataon.hyeyum.service.KsicInfoTextImportService;

@Validated
@RestController
@RequestMapping("/admin/imports")
@Tag(name = "Admin Imports", description = "관리자 데이터 import API")
public class AdminDataImportController {

    private final KodataExcelImportService kodataExcelImportService;
    private final BtpSupportExcelImportService btpSupportExcelImportService;
    private final BtpEquipmentTextImportService btpEquipmentTextImportService;
    private final DummyDataGenerationService dummyDataGenerationService;
    private final CompanyMetricCalculationService companyMetricCalculationService;
    private final KsicInfoTextImportService ksicInfoTextImportService;

    public AdminDataImportController(
            KodataExcelImportService kodataExcelImportService,
            BtpSupportExcelImportService btpSupportExcelImportService,
            BtpEquipmentTextImportService btpEquipmentTextImportService,
            DummyDataGenerationService dummyDataGenerationService,
            CompanyMetricCalculationService companyMetricCalculationService,
            KsicInfoTextImportService ksicInfoTextImportService) {
        this.kodataExcelImportService = kodataExcelImportService;
        this.btpSupportExcelImportService = btpSupportExcelImportService;
        this.btpEquipmentTextImportService = btpEquipmentTextImportService;
        this.dummyDataGenerationService = dummyDataGenerationService;
        this.companyMetricCalculationService = companyMetricCalculationService;
        this.ksicInfoTextImportService = ksicInfoTextImportService;
    }

    @PostMapping(value = "/kodata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "KODATA Excel import")
    public DataImportResult importKodata(@RequestPart("file") @NotNull MultipartFile file) {
        return kodataExcelImportService.importFile(file);
    }

    @PostMapping(value = "/btp-supports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "BTP 지원사업 Excel import")
    public DataImportResult importBtpSupports(@RequestPart("file") @NotNull MultipartFile file) {
        return btpSupportExcelImportService.importFile(file);
    }

    @PostMapping(value = "/btp-equipment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "BTP 장비 텍스트 import")
    public DataImportResult importBtpEquipment(@RequestPart("file") @NotNull MultipartFile file) {
        return btpEquipmentTextImportService.importFile(file);
    }

    @PostMapping(value = "/ksic-info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "KSIC 업종 설명 import")
    public DataImportResult importKsicInfo(@RequestPart("file") @NotNull MultipartFile file) {
        return ksicInfoTextImportService.importFile(file);
    }

    @PostMapping(value = "/dummy-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "더미 기업 데이터 생성")
    public DummyDataGenerationResult generateDummyData(
            @RequestPart("announcementPdf") @NotNull MultipartFile announcementPdf,
            @RequestParam("year") @NotNull Integer year) {
        return dummyDataGenerationService.generate(announcementPdf, year);
    }

    @GetMapping("/company-metrics")
    @Operation(summary = "기업 계산 지표 재계산")
    public CompanyMetricRecalculationResult recalculateCompanyMetrics() {
        return companyMetricCalculationService.recalculateAllWithoutAi();
    }
}
