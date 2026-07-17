package site.dataon.hyeyum.controller;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.dto.DataImportResult;
import site.dataon.hyeyum.service.BtpSupportExcelImportService;
import site.dataon.hyeyum.service.KodataExcelImportService;

@Validated
@RestController
@RequestMapping("/api/admin/imports")
public class AdminDataImportController {

    private final KodataExcelImportService kodataExcelImportService;
    private final BtpSupportExcelImportService btpSupportExcelImportService;

    public AdminDataImportController(
            KodataExcelImportService kodataExcelImportService,
            BtpSupportExcelImportService btpSupportExcelImportService) {
        this.kodataExcelImportService = kodataExcelImportService;
        this.btpSupportExcelImportService = btpSupportExcelImportService;
    }

    @PostMapping(value = "/kodata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DataImportResult importKodata(@RequestPart("file") @NotNull MultipartFile file) {
        return kodataExcelImportService.importFile(file);
    }

    @PostMapping(value = "/btp-supports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DataImportResult importBtpSupports(@RequestPart("file") @NotNull MultipartFile file) {
        return btpSupportExcelImportService.importFile(file);
    }
}
