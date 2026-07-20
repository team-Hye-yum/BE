package site.dataon.hyeyum.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
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
import site.dataon.hyeyum.dto.YearlyCount;
import site.dataon.hyeyum.dto.YearlyMoneyAmount;
import site.dataon.hyeyum.repository.BtpSupportProgramRepository;
import site.dataon.hyeyum.repository.SupportProgramCompanyProjection;

@Service
public class SupportProgramPageService {

    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String KRW_THOUSAND = "KRW_THOUSAND";
    private static final String[] COMPANY_LIST_HEADERS = {
        "기업명",
        "사업자번호(일련번호)",
        "지역",
        "설립연도",
        "업종",
        "주요제품",
        "최근 매출",
        "종업원 수",
        "특허 등록 누적 수",
        "NTIS 수행건수",
        "지원 횟수",
        "사업명",
        "누적 지원금",
        "지원연도",
        "부채 비율",
        "매출 성장성"
    };
    private static final String[] FALLBACK_TEMPLATE_HEADERS = {
        "기업일련번호", "기업명", "사업자등록번호", "지역", "설립일자", "업종코드", "업종명", "주요제품", "주소"
    };
    private static final String COMPANY_TEMPLATE_PATH = "templates/company-info-template.xlsx";

    private final JdbcTemplate jdbcTemplate;
    private final BtpSupportProgramRepository supportProgramRepository;
    private final OpenAiSupportProgramAnalysisClient analysisClient;
    private final KodataExcelImportService kodataExcelImportService;

    public SupportProgramPageService(
            JdbcTemplate jdbcTemplate,
            BtpSupportProgramRepository supportProgramRepository,
            OpenAiSupportProgramAnalysisClient analysisClient,
            KodataExcelImportService kodataExcelImportService) {
        this.jdbcTemplate = jdbcTemplate;
        this.supportProgramRepository = supportProgramRepository;
        this.analysisClient = analysisClient;
        this.kodataExcelImportService = kodataExcelImportService;
    }

    public ApiDataResponse<SupportProgramSearchResponse> search(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<SupportProgramSearchItem> items = supportProgramRepository.search(normalizedKeyword).stream()
                .map(this::mapSearchItem)
                .toList();
        return new ApiDataResponse<>(new SupportProgramSearchResponse(items));
    }

    public ApiDataResponse<SupportProgramCompanyListResponse> companies(Long supportProgramId) {
        BtpSupportProgram supportProgram = supportProgram(supportProgramId);
        List<SupportProgramCompanyItem> items = supportProgramRepository
                .findCompaniesForProgram(
                        supportProgram.getCode(),
                        supportProgram.getProgramYear(),
                        supportProgram.getBudgetProgramName())
                .stream()
                .map(this::mapCompanyItem)
                .toList();
        return new ApiDataResponse<>(new SupportProgramCompanyListResponse(items));
    }

    public byte[] companyListExcel(Long supportProgramId) {
        List<SupportProgramCompanyItem> items = companies(supportProgramId).data().items();
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
            for (SupportProgramCompanyItem item : items) {
                Row row = sheet.createRow(rowIndex++);
                write(row, 0, item.companyName());
                write(row, 1, item.businessRegistrationNumberMasked() == null ? item.companyId() : item.businessRegistrationNumberMasked());
                write(row, 2, item.region());
                write(row, 3, item.establishedYear());
                write(row, 4, item.industryName());
                write(row, 5, item.mainProduct());
                write(row, 6, item.latestSalesAmount() == null ? null : item.latestSalesAmount().value());
                write(row, 7, item.employeeCount() == null ? null : item.employeeCount().value());
                write(row, 8, item.registeredPatentCount());
                write(row, 9, item.ntisProjectCount());
                write(row, 10, item.supportCount());
                write(row, 11, item.programName());
                write(row, 12, item.cumulativeSupportAmount() == null ? null : item.cumulativeSupportAmount().value());
                write(row, 13, joinYears(item.supportYears()));
                write(row, 14, item.debtRatio());
                write(row, 15, item.salesGrowthRate());
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
        return new ApiDataResponse<>(new SupportProgramSaveResponse(savedProgram.getSupportProgramId(), !exists));
    }

    public ApiDataResponse<CompanyTemplateImportResponse> importCompanyTemplate(MultipartFile file) {
        int existingCompanies = countCompanies();
        int dataRows = countTemplateRows(file);
        var result = kodataExcelImportService.importFile(file);
        int currentCompanies = countCompanies();
        int createdCompanies = Math.max(0, currentCompanies - existingCompanies);
        int updatedCompanies = Math.max(0, dataRows - createdCompanies);
        return new ApiDataResponse<>(new CompanyTemplateImportResponse(dataRows, createdCompanies, updatedCompanies, List.of()));
    }

    private SupportProgramSearchItem mapSearchItem(BtpSupportProgram program) {
        return new SupportProgramSearchItem(
                program.getSupportProgramId(),
                program.getCode(),
                program.getProgramYear(),
                program.getBudgetProgramName(),
                program.getProgramCategory(),
                program.getSupportType(),
                new SupportProgramPeriod(compactDate(program.getStartDate()), compactDate(program.getEndDate())),
                program.getDepartmentName(),
                program.getLocalGovernmentName());
    }

    private SupportProgramCompanyItem mapCompanyItem(SupportProgramCompanyProjection projection) {
        return new SupportProgramCompanyItem(
                projection.getCompanyId(),
                projection.getCompanyName(),
                maskBusinessRegistrationNumber(projection.getBusinessRegistrationNumber()),
                projection.getRegionName(),
                projection.getEstablishedYear(),
                projection.getIndustryName(),
                projection.getMainProduct(),
                new YearlyMoneyAmount(projection.getLatestSalesAmount(), KRW_THOUSAND, projection.getLatestSalesYear()),
                new YearlyCount(projection.getEmployeeCount(), projection.getEmployeeYear()),
                projection.getRegisteredPatentCount(),
                projection.getNtisProjectCount(),
                projection.getSupportCount(),
                projection.getProgramName(),
                new MoneyAmount(projection.getCumulativeSupportAmount(), KRW_THOUSAND),
                parseYears(projection.getSupportYears()),
                round(projection.getDebtRatio()),
                round(projection.getSalesGrowthRate()));
    }

    private BtpSupportProgram supportProgram(Long supportProgramId) {
        return supportProgramRepository
                .findById(supportProgramId)
                .orElseThrow(() -> new IllegalArgumentException("지원 사업을 찾을 수 없습니다. supportProgramId=" + supportProgramId));
    }

    private int countCompanies() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from company", Integer.class);
        return count == null ? 0 : count;
    }

    private int countTemplateRows(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet("1__기업정보");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            }
            if (sheet == null) {
                return 0;
            }
            int count = 0;
            for (Row row : sheet) {
                if (row.getRowNum() < 5) {
                    continue;
                }
                if (ExcelImportSupport.integer(row, 0) != null) {
                    count++;
                }
            }
            return count;
        } catch (IOException exception) {
            throw new IllegalArgumentException("기업정보 Excel 템플릿을 읽을 수 없습니다.", exception);
        }
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

    private List<Integer> parseYears(String years) {
        if (years == null || years.isBlank()) {
            return List.of();
        }
        return Arrays.stream(years.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Integer::valueOf)
                .toList();
    }

    private String joinYears(List<Integer> years) {
        if (years == null || years.isEmpty()) {
            return "";
        }
        return String.join(", ", years.stream().map(String::valueOf).toList());
    }

    private String maskBusinessRegistrationNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 10) {
            return value;
        }
        return digits.substring(0, 3) + "-" + digits.substring(3, 5) + "-*****";
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
