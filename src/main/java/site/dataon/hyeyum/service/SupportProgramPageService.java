package site.dataon.hyeyum.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.domain.BtpSupportProgram;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyTemplateImportResponse;
import site.dataon.hyeyum.dto.MoneyAmount;
import site.dataon.hyeyum.dto.SupportProgramAnalysisPayload;
import site.dataon.hyeyum.dto.SupportProgramAnnouncementAnalysisResponse;
import site.dataon.hyeyum.dto.SupportProgramCompanyItem;
import site.dataon.hyeyum.dto.SupportProgramCompanyListResponse;
import site.dataon.hyeyum.dto.SupportProgramPeriod;
import site.dataon.hyeyum.dto.SupportProgramSaveRequest;
import site.dataon.hyeyum.dto.SupportProgramSaveResponse;
import site.dataon.hyeyum.dto.SupportProgramSearchItem;
import site.dataon.hyeyum.dto.SupportProgramSearchResponse;
import site.dataon.hyeyum.dto.YearlyMoneyAmount;
import site.dataon.hyeyum.repository.BtpSupportProgramRepository;
import site.dataon.hyeyum.repository.SupportProgramCompanyProjection;

@Service
public class SupportProgramPageService {

    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String KRW_THOUSAND = "KRW_THOUSAND";
    private static final String[] COMPANY_LIST_HEADERS = {
        "기업명",
        "지역",
        "설립연도",
        "업종",
        "주요제품",
        "최근 매출",
        "종업원 수",
        "특허 등록 누적 수",
        "NTIS 수행건수",
        "지원 횟수",
        "누적 지원금",
        "부채 비율",
        "매출 성장성"
    };
    private static final String[] FALLBACK_TEMPLATE_HEADERS = {
        "기업일련번호", "기업명", "사업자등록번호", "지역", "설립일자", "업종코드", "업종명", "주요제품", "주소"
    };
    private static final String COMPANY_TEMPLATE_PATH = "templates/company-info-template.xlsx";

    private final BtpSupportProgramRepository supportProgramRepository;
    private final OpenAiSupportProgramAnalysisClient analysisClient;
    private final CompanyTemplateImportService companyTemplateImportService;

    public SupportProgramPageService(
            BtpSupportProgramRepository supportProgramRepository,
            OpenAiSupportProgramAnalysisClient analysisClient,
            CompanyTemplateImportService companyTemplateImportService) {
        this.supportProgramRepository = supportProgramRepository;
        this.analysisClient = analysisClient;
        this.companyTemplateImportService = companyTemplateImportService;
    }

    public ApiDataResponse<SupportProgramSearchResponse> search(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<SupportProgramSearchItem> items = supportProgramRepository.search(normalizedKeyword).stream()
                .map(this::mapSearchItem)
                .toList();
        return new ApiDataResponse<>(new SupportProgramSearchResponse(items));
    }

    public ApiDataResponse<SupportProgramCompanyListResponse> companies(String supportProgramCode) {
        List<SupportProgramCompanyItem> items = companyProjections(supportProgramCode)
                .stream()
                .map(this::mapCompanyItem)
                .toList();
        return new ApiDataResponse<>(new SupportProgramCompanyListResponse(items));
    }

    public byte[] companyListExcel(String supportProgramCode) {
        List<SupportProgramCompanyProjection> items = companyProjections(supportProgramCode);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("사업별 기업 목록");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            Row header = sheet.createRow(0);
            for (int index = 0; index < COMPANY_LIST_HEADERS.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(COMPANY_LIST_HEADERS[index]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (SupportProgramCompanyProjection item : items) {
                Row row = sheet.createRow(rowIndex++);
                write(row, 0, item.getCompanyName());
                write(row, 1, item.getRegionName());
                write(row, 2, item.getEstablishedYear());
                write(row, 3, item.getIndustryName());
                write(row, 4, item.getMainProduct());
                write(row, 5, item.getLatestSalesAmount());
                write(row, 6, item.getEmployeeCount());
                write(row, 7, item.getRegisteredPatentCount());
                write(row, 8, item.getNtisProjectCount());
                write(row, 9, item.getSupportCount());
                write(row, 10, item.getCumulativeSupportAmount());
                write(row, 11, round(item.getDebtRatio()));
                write(row, 12, round(item.getSalesGrowthRate()));
            }
            for (int index = 0; index < COMPANY_LIST_HEADERS.length; index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("사업별 기업 목록 Excel을 생성할 수 없습니다.", exception);
        }
    }

    public byte[] companyTemplate() {
        ClassPathResource template = new ClassPathResource(COMPANY_TEMPLATE_PATH);
        if (template.exists()) {
            try (InputStream inputStream = template.getInputStream()) {
                return inputStream.readAllBytes();
            } catch (IOException exception) {
                throw new IllegalStateException("기업정보 템플릿 파일을 읽을 수 없습니다.", exception);
            }
        }
        return fallbackCompanyTemplate();
    }

    public ApiDataResponse<SupportProgramAnnouncementAnalysisResponse> analyzeAnnouncement(MultipartFile announcementPdf) {
        String extractedText = extractPdfText(announcementPdf);
        SupportProgramAnalysisPayload analysis = analysisClient.analyzeAnnouncement(announcementPdf);
        return new ApiDataResponse<>(
                new SupportProgramAnnouncementAnalysisResponse(preview(extractedText), analysis));
    }

    @Transactional
    public ApiDataResponse<SupportProgramSaveResponse> save(SupportProgramSaveRequest request) {
        String code = limit(request.code(), 20);
        BtpSupportProgram supportProgram =
                supportProgramRepository.findByProgramYearAndCode(request.programYear(), code).orElse(null);
        boolean exists = supportProgram != null;
        LocalDate startDate = parseCompactDate(request.period() == null ? null : request.period().startDate());
        LocalDate endDate = parseCompactDate(request.period() == null ? null : request.period().endDate());
        if (exists) {
            supportProgram.update(
                    limit(request.budgetProgramName(), 1000),
                    limit(request.programCategory(), 20),
                    limit(request.supportType(), 20),
                    startDate,
                    endDate,
                    limit(request.departmentName(), 20),
                    limit(request.localGovernmentName(), 20),
                    limit(request.programSummary(), 1000));
        } else {
            supportProgram =
                    BtpSupportProgram.create(
                            code,
                            request.programYear(),
                            limit(request.budgetProgramName(), 1000),
                            limit(request.programCategory(), 20),
                            limit(request.supportType(), 20),
                            startDate,
                            endDate,
                            limit(request.departmentName(), 20),
                            limit(request.localGovernmentName(), 20),
                            limit(request.programSummary(), 1000));
        }
        BtpSupportProgram savedProgram = supportProgramRepository.save(supportProgram);
        return new ApiDataResponse<>(new SupportProgramSaveResponse(savedProgram.getCode(), !exists));
    }

    public ApiDataResponse<CompanyTemplateImportResponse> importCompanyTemplate(MultipartFile file) {
        return new ApiDataResponse<>(companyTemplateImportService.importFile(file));
    }

    private SupportProgramSearchItem mapSearchItem(BtpSupportProgram program) {
        return new SupportProgramSearchItem(
                program.getCode(),
                program.getProgramYear(),
                program.getBudgetProgramName(),
                program.getProgramCategory(),
                program.getSupportType(),
                new SupportProgramPeriod(compactDate(program.getStartDate()), compactDate(program.getEndDate())),
                program.getDepartmentName(),
                program.getLocalGovernmentName());
    }

    private List<SupportProgramCompanyProjection> companyProjections(String supportProgramCode) {
        BtpSupportProgram supportProgram = supportProgram(supportProgramCode);
        return supportProgramRepository.findCompaniesForProgram(
                supportProgram.getCode(),
                supportProgram.getProgramYear(),
                supportProgram.getBudgetProgramName());
    }

    private SupportProgramCompanyItem mapCompanyItem(SupportProgramCompanyProjection projection) {
        return new SupportProgramCompanyItem(
                projection.getCompanyId(),
                projection.getCompanyName(),
                projection.getRegionName(),
                projection.getEstablishedYear(),
                projection.getIndustryName(),
                projection.getMainProduct(),
                new YearlyMoneyAmount(projection.getLatestSalesAmount(), KRW_THOUSAND, projection.getLatestSalesYear()),
                projection.getEmployeeCount(),
                projection.getRegisteredPatentCount(),
                projection.getNtisProjectCount(),
                projection.getSupportCount(),
                new MoneyAmount(projection.getCumulativeSupportAmount(), KRW_THOUSAND),
                round(projection.getDebtRatio()),
                round(projection.getSalesGrowthRate()));
    }

    private BtpSupportProgram supportProgram(String supportProgramCode) {
        String normalizedCode = supportProgramCode == null ? "" : supportProgramCode.trim();
        return supportProgramRepository
                .findFirstByCodeOrderByProgramYearDescSupportProgramIdDesc(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("지원 사업을 찾을 수 없습니다. supportProgramCode=" + supportProgramCode));
    }

    private byte[] fallbackCompanyTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("1__기업정보");
            Row header = sheet.createRow(0);
            for (int index = 0; index < FALLBACK_TEMPLATE_HEADERS.length; index++) {
                header.createCell(index).setCellValue(FALLBACK_TEMPLATE_HEADERS[index]);
                sheet.autoSizeColumn(index);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("기업정보 템플릿을 생성할 수 없습니다.", exception);
        }
    }

    private String extractPdfText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException exception) {
            return "";
        }
    }

    private String preview(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }

    private String compactDate(LocalDate date) {
        return date == null ? null : date.format(COMPACT_DATE);
    }

    private LocalDate parseCompactDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() == 8 && value.chars().allMatch(Character::isDigit)) {
            return LocalDate.parse(value, COMPACT_DATE);
        }
        return LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
    }

    private Double round(Double value) {
        return value == null ? null : Math.round(value * 100.0) / 100.0;
    }

    private void write(Row row, int index, Object value) {
        Cell cell = row.createCell(index);
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
