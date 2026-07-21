package site.dataon.hyeyum.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.common.SupportSelectionResults;
import site.dataon.hyeyum.dto.CompanyTemplateImportResponse;

@Service
public class CompanyTemplateImportService {

    private static final Logger log = LoggerFactory.getLogger(CompanyTemplateImportService.class);
    private static final int PROGRESS_LOG_INTERVAL = 100;
    private static final int[] YEARS = {2020, 2021, 2022, 2023, 2024};

    private final JdbcTemplate jdbcTemplate;
    private final CompanyMetricCalculationService companyMetricCalculationService;

    public CompanyTemplateImportService(
            JdbcTemplate jdbcTemplate,
            CompanyMetricCalculationService companyMetricCalculationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.companyMetricCalculationService = companyMetricCalculationService;
    }

    @Transactional
    public CompanyTemplateImportResponse importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("기업정보 Excel 파일이 비어 있습니다.");
        }
        long startedAt = System.nanoTime();
        log.info(
                "Company template import started. fileName={}, size={} bytes",
                file.getOriginalFilename(),
                file.getSize());
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            log.info("Company template workbook loaded. sheets={}", workbook.getNumberOfSheets());
            Sheet sheet = companySheet(workbook);
            Header header = header(sheet);
            SupportProgramInfo supportProgram = supportProgram(workbook);
            CompanyTemplateImportResult result = importRows(sheet, header, supportProgram);
            importDetailSheets(workbook);
            int metricUpdatedCompanies = companyMetricCalculationService.recalculateNumericMetrics(result.companyIds());
            companyMetricCalculationService.generateAiTextsAsync(result.companyIds());
            CompanyTemplateImportResponse response = result.toResponse(metricUpdatedCompanies);
            log.info(
                    "Company template import finished. importedRows={}, createdCompanies={}, updatedCompanies={}, supportHistoryRows={}, metricUpdatedCompanies={}, errors={}, elapsedMs={}",
                    response.importedRows(),
                    response.createdCompanies(),
                    response.updatedCompanies(),
                    response.supportHistoryRows(),
                    response.metricUpdatedCompanies(),
                    response.errors().size(),
                    elapsedMillis(startedAt));
            return response;
        } catch (IOException exception) {
            throw new IllegalArgumentException("기업정보 Excel 템플릿을 읽을 수 없습니다.", exception);
        }
    }

    private Sheet companySheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet("1__기업정보");
        if (sheet != null) {
            log.info(
                    "Company template company sheet selected. sheetName={}, lastRowNum={}, physicalRows={}",
                    sheet.getSheetName(),
                    sheet.getLastRowNum(),
                    sheet.getPhysicalNumberOfRows());
            return sheet;
        }
        if (workbook.getNumberOfSheets() == 0) {
            throw new IllegalArgumentException("기업정보 Excel 시트가 없습니다.");
        }
        Sheet fallbackSheet = workbook.getSheetAt(0);
        log.warn(
                "Company template sheet '1__기업정보' not found. Using first sheet. sheetName={}, lastRowNum={}, physicalRows={}",
                fallbackSheet.getSheetName(),
                fallbackSheet.getLastRowNum(),
                fallbackSheet.getPhysicalNumberOfRows());
        return fallbackSheet;
    }

    private Header header(Sheet sheet) {
        log.info("Company template header search started. sheetName={}", sheet.getSheetName());
        for (Row row : sheet) {
            Map<String, List<Integer>> columns = new HashMap<>();
            for (int index = 0; index < row.getLastCellNum(); index++) {
                String key = normalized(ExcelImportSupport.text(row, index));
                if (key != null) {
                    columns.computeIfAbsent(key, ignored -> new ArrayList<>()).add(index);
                }
            }
            if (columns.containsKey("기업일련번호")) {
                log.info(
                        "Company template header found. sheetName={}, excelRow={}, columns={}",
                        sheet.getSheetName(),
                        row.getRowNum() + 1,
                        columns.size());
                return new Header(row.getRowNum(), columns);
            }
        }
        throw new IllegalArgumentException("기업정보 Excel에서 '기업일련번호' 헤더를 찾을 수 없습니다.");
    }

    private SupportProgramInfo supportProgram(Workbook workbook) {
        String code = supportProgramCode(workbook);
        log.info("Company template support program lookup started. code={}", code);
        try {
            SupportProgramInfo supportProgram = jdbcTemplate.queryForObject(
                    """
                    select program_year, code, budget_program_name, support_type, start_date, end_date
                    from btp_support_program
                    where code = ?
                    order by program_year desc, support_program_id desc
                    limit 1
                    """,
                    (rs, rowNum) -> new SupportProgramInfo(
                            rs.getInt("program_year"),
                            rs.getString("code"),
                            rs.getString("budget_program_name"),
                            rs.getString("support_type"),
                            rs.getObject("start_date", LocalDate.class),
                            rs.getObject("end_date", LocalDate.class)),
                    code);
            log.info(
                    "Company template support program found. code={}, supportYear={}, budgetProgramName={}",
                    supportProgram.code(),
                    supportProgram.supportYear(),
                    supportProgram.budgetProgramName());
            return supportProgram;
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("지원사업코드를 찾을 수 없습니다. code=" + code);
        }
    }

    private String supportProgramCode(Workbook workbook) {
        Sheet sheet = workbook.getSheet("0__지원사업번호");
        if (sheet == null) {
            throw new IllegalArgumentException("지원사업번호 시트가 없습니다.");
        }
        log.info("Company template support program code sheet scan started. sheetName={}", sheet.getSheetName());
        for (Row row : sheet) {
            for (int index = 0; index < row.getLastCellNum(); index++) {
                String value = ExcelImportSupport.text(row, index);
                if (value == null || "지원사업정보코드".equals(normalized(value))) {
                    continue;
                }
                return value;
            }
        }
        throw new IllegalArgumentException("지원사업번호 시트에 지원사업코드를 입력해야 합니다.");
    }

    private CompanyTemplateImportResult importRows(Sheet sheet, Header header, SupportProgramInfo supportProgram) {
        long startedAt = System.nanoTime();
        int importedRows = 0;
        int createdCompanies = 0;
        int updatedCompanies = 0;
        int supportHistoryRows = 0;
        List<String> errors = new ArrayList<>();
        Set<Integer> importedCompanyIds = new HashSet<>();
        Set<Integer> existingCompanyIds = existingCompanyIds(sheet, header);
        log.info(
                "Company template row import started. sheetName={}, headerExcelRow={}, lastRowNum={}, existingCompanies={}",
                sheet.getSheetName(),
                header.rowIndex() + 1,
                sheet.getLastRowNum(),
                existingCompanyIds.size());
        for (Row row : sheet) {
            if (row.getRowNum() <= header.rowIndex()) {
                continue;
            }
            try {
                Integer companyId = integer(row, header.column("기업일련번호"));
                if (companyId == null) {
                    continue;
                }
                long rowStartedAt = System.nanoTime();
                boolean exists = existingCompanyIds.contains(companyId);
                upsertCompany(row, header, companyId);
                upsertAnnualStatistics(row, companyId);
                upsertSupportHistory(row, header, companyId, supportProgram);
                importedCompanyIds.add(companyId);
                importedRows++;
                supportHistoryRows++;
                if (exists) {
                    updatedCompanies++;
                } else {
                    createdCompanies++;
                }
                long rowElapsedMs = elapsedMillis(rowStartedAt);
                if (importedRows % PROGRESS_LOG_INTERVAL == 0) {
                    log.info(
                            "Company template import progress. processedRows={}, currentExcelRow={}, companyId={}, elapsedMs={}",
                            importedRows,
                            row.getRowNum() + 1,
                            companyId,
                            elapsedMillis(startedAt));
                }
                if (rowElapsedMs >= 1_000) {
                    log.warn(
                            "Company template row import is slow. excelRow={}, companyId={}, elapsedMs={}",
                            row.getRowNum() + 1,
                            companyId,
                            rowElapsedMs);
                }
            } catch (RuntimeException exception) {
                log.warn("Company template row import failed. excelRow={}", row.getRowNum() + 1, exception);
                errors.add("%d행: %s".formatted(row.getRowNum() + 1, exception.getMessage()));
            }
        }
        log.info(
                "Company template row import completed. importedRows={}, supportHistoryRows={}, errors={}, elapsedMs={}",
                importedRows,
                supportHistoryRows,
                errors.size(),
                elapsedMillis(startedAt));
        return new CompanyTemplateImportResult(
                importedRows,
                createdCompanies,
                updatedCompanies,
                supportHistoryRows,
                importedCompanyIds,
                errors);
    }

    private Set<Integer> existingCompanyIds(Sheet sheet, Header header) {
        long startedAt = System.nanoTime();
        Set<Integer> companyIds = new HashSet<>();
        for (Row row : sheet) {
            if (row.getRowNum() <= header.rowIndex()) {
                continue;
            }
            try {
                Integer companyId = integer(row, header.column("기업일련번호"));
                if (companyId != null) {
                    companyIds.add(companyId);
                }
            } catch (RuntimeException ignored) {
                // The row-level import loop reports invalid rows with the exact Excel row number.
            }
        }
        if (companyIds.isEmpty()) {
            log.info("Company template existing company lookup skipped. candidateCompanies=0");
            return Set.of();
        }
        String placeholders = String.join(",", companyIds.stream().map(ignored -> "?").toList());
        Set<Integer> existingCompanyIds = new HashSet<>(jdbcTemplate.query(
                "select company_id from company where company_id in (" + placeholders + ")",
                (rs, rowNum) -> rs.getInt("company_id"),
                companyIds.toArray()));
        log.info(
                "Company template existing company lookup finished. candidateCompanies={}, existingCompanies={}, elapsedMs={}",
                companyIds.size(),
                existingCompanyIds.size(),
                elapsedMillis(startedAt));
        return existingCompanyIds;
    }

    private void upsertCompany(Row row, Header header, Integer companyId) {
        jdbcTemplate.update(
                """
                insert into company (
                    company_id, company_name, business_registration_number, region_name, established_date,
                    ksic_code, industry_name, main_product, address, business_entity_type, company_size,
                    listing_status, company_type, is_closed, closed_date, reference_date, closure_type,
                    company_status, is_innobiz, is_mainbiz, is_venture_company, is_materials_company,
                    is_net_certified, is_nep_certified, researcher_count, has_research_lab,
                    research_lab_registered_date, has_rnd_department, rnd_department_registered_date
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (company_id) do update set
                    company_name = coalesce(excluded.company_name, company.company_name),
                    business_registration_number = coalesce(excluded.business_registration_number, company.business_registration_number),
                    region_name = coalesce(excluded.region_name, company.region_name),
                    established_date = coalesce(excluded.established_date, company.established_date),
                    ksic_code = coalesce(excluded.ksic_code, company.ksic_code),
                    industry_name = coalesce(excluded.industry_name, company.industry_name),
                    main_product = coalesce(excluded.main_product, company.main_product),
                    address = coalesce(excluded.address, company.address),
                    business_entity_type = coalesce(excluded.business_entity_type, company.business_entity_type),
                    company_size = coalesce(excluded.company_size, company.company_size),
                    listing_status = coalesce(excluded.listing_status, company.listing_status),
                    company_type = coalesce(excluded.company_type, company.company_type),
                    is_closed = coalesce(excluded.is_closed, company.is_closed),
                    closed_date = coalesce(excluded.closed_date, company.closed_date),
                    reference_date = coalesce(excluded.reference_date, company.reference_date),
                    closure_type = coalesce(excluded.closure_type, company.closure_type),
                    company_status = coalesce(excluded.company_status, company.company_status),
                    is_innobiz = coalesce(excluded.is_innobiz, company.is_innobiz),
                    is_mainbiz = coalesce(excluded.is_mainbiz, company.is_mainbiz),
                    is_venture_company = coalesce(excluded.is_venture_company, company.is_venture_company),
                    is_materials_company = coalesce(excluded.is_materials_company, company.is_materials_company),
                    is_net_certified = coalesce(excluded.is_net_certified, company.is_net_certified),
                    is_nep_certified = coalesce(excluded.is_nep_certified, company.is_nep_certified),
                    researcher_count = coalesce(excluded.researcher_count, company.researcher_count),
                    has_research_lab = coalesce(excluded.has_research_lab, company.has_research_lab),
                    research_lab_registered_date = coalesce(excluded.research_lab_registered_date, company.research_lab_registered_date),
                    has_rnd_department = coalesce(excluded.has_rnd_department, company.has_rnd_department),
                    rnd_department_registered_date = coalesce(excluded.rnd_department_registered_date, company.rnd_department_registered_date)
                """,
                companyId,
                text(row, header.column("기업명")),
                text(row, header.column("사업자등록번호")),
                text(row, header.column("지역")),
                date(row, header.column("설립일자")),
                text(row, header.column("KSIC코드(11차)")),
                text(row, header.column("업종명(11차)")),
                text(row, header.column("주요제품")),
                text(row, header.column("주소")),
                text(row, header.column("기업유형(법인/개인)")),
                text(row, header.column("기업규모(대/중/소)")),
                text(row, header.column("기업공개(코스피,코스닥)")),
                text(row, header.column("기업형태(주식/개인)")),
                koreanBoolean(row, header.column("휴폐업여부")),
                date(row, header.column("폐업일자")),
                date(row, header.column("조회일자")),
                text(row, header.column("휴폐업구분")),
                text(row, header.column("기업상태")),
                koreanBoolean(row, header.columnOrDefault("이노비즈", 90)),
                koreanBoolean(row, header.columnOrDefault("메인비즈", 91)),
                koreanBoolean(row, header.columnOrDefault("벤처기업", 92)),
                koreanBoolean(row, header.columnOrDefault("소재부품", 93)),
                koreanBoolean(row, header.columnOrDefault("NET", 94)),
                koreanBoolean(row, header.columnOrDefault("NEP", 95)),
                integer(row, header.columnOrDefault("연구원수", 106)),
                koreanBoolean(row, header.columnOrDefault("기업부설연구소", 107)),
                date(row, header.columnOrDefault("기업부설연구소등록일", 108)),
                koreanBoolean(row, header.columnOrDefault("연구개발전담부서", 109)),
                date(row, header.columnOrDefault("연구개발전담부서등록일", 110)));
    }

    private void upsertSupportHistory(Row row, Header header, Integer companyId, SupportProgramInfo supportProgram) {
        String sourceHash = ExcelImportSupport.hash("company-template", supportProgram.supportYear(), supportProgram.code(), companyId);
        jdbcTemplate.update(
                """
                insert into btp_support_history (
                    support_year, code, budget_program_name, support_type, start_date, end_date,
                    selection_result, company_id, industry_code, province_name, main_product, established_year, source_hash
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (source_hash) do update set
                    budget_program_name = excluded.budget_program_name,
                    support_type = excluded.support_type,
                    start_date = excluded.start_date,
                    end_date = excluded.end_date,
                    selection_result = excluded.selection_result,
                    industry_code = excluded.industry_code,
                    province_name = excluded.province_name,
                    main_product = excluded.main_product,
                    established_year = excluded.established_year
                """,
                supportProgram.supportYear(),
                supportProgram.code(),
                supportProgram.budgetProgramName(),
                supportProgram.supportType(),
                supportProgram.startDate(),
                supportProgram.endDate(),
                SupportSelectionResults.SELECTED,
                companyId,
                text(row, header.column("KSIC코드(11차)")),
                text(row, header.column("지역")),
                text(row, header.column("주요제품")),
                establishedYear(row, header),
                sourceHash);
    }

    private void upsertAnnualStatistics(Row row, Integer companyId) {
        for (int index = 0; index < YEARS.length; index++) {
            int year = YEARS[index];
            upsertEmployment(row, companyId, year, index);
            upsertFinancial(row, companyId, year, index);
            upsertPatentStatistics(row, companyId, year, index);
        }
    }

    private void upsertEmployment(Row row, Integer companyId, int year, int offset) {
        jdbcTemplate.update(
                """
                insert into company_employment_statistics (
                    company_id, year, employee_count, pension_subscriber_count,
                    pension_new_hire_count, pension_retiree_count, average_salary
                ) values (?, ?, ?, ?, ?, ?, ?)
                on conflict (company_id, year) do update set
                    employee_count = excluded.employee_count,
                    pension_subscriber_count = excluded.pension_subscriber_count,
                    pension_new_hire_count = excluded.pension_new_hire_count,
                    pension_retiree_count = excluded.pension_retiree_count,
                    average_salary = excluded.average_salary
                """,
                companyId,
                year,
                ExcelImportSupport.integer(row, 15 + offset),
                ExcelImportSupport.integer(row, 20 + offset),
                ExcelImportSupport.integer(row, 25 + offset),
                ExcelImportSupport.integer(row, 30 + offset),
                ExcelImportSupport.integer(row, 35 + offset));
    }

    private void upsertFinancial(Row row, Integer companyId, int year, int offset) {
        jdbcTemplate.update(
                """
                insert into company_financial_statistics (
                    company_id, year, sales_amount, operating_income, cost_of_sales,
                    net_income, operating_margin, total_assets, total_liabilities,
                    total_equity, paid_in_capital, research_and_development_expense
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (company_id, year) do update set
                    sales_amount = excluded.sales_amount,
                    operating_income = excluded.operating_income,
                    cost_of_sales = excluded.cost_of_sales,
                    net_income = excluded.net_income,
                    operating_margin = excluded.operating_margin,
                    total_assets = excluded.total_assets,
                    total_liabilities = excluded.total_liabilities,
                    total_equity = excluded.total_equity,
                    paid_in_capital = excluded.paid_in_capital,
                    research_and_development_expense = excluded.research_and_development_expense
                """,
                companyId,
                year,
                ExcelImportSupport.integer(row, 40 + offset),
                ExcelImportSupport.integer(row, 45 + offset),
                ExcelImportSupport.integer(row, 50 + offset),
                ExcelImportSupport.integer(row, 55 + offset),
                ExcelImportSupport.decimal(row, 60 + offset),
                ExcelImportSupport.integer(row, 65 + offset),
                ExcelImportSupport.integer(row, 70 + offset),
                ExcelImportSupport.integer(row, 75 + offset),
                ExcelImportSupport.integer(row, 80 + offset),
                ExcelImportSupport.integer(row, 85 + offset));
    }

    private void upsertPatentStatistics(Row row, Integer companyId, int year, int offset) {
        jdbcTemplate.update(
                """
                insert into company_patent_statistics (
                    company_id, year, registered_patent_count, patent_application_count
                ) values (?, ?, ?, ?)
                on conflict (company_id, year) do update set
                    registered_patent_count = excluded.registered_patent_count,
                    patent_application_count = excluded.patent_application_count
                """,
                companyId,
                year,
                ExcelImportSupport.integer(row, 96 + offset),
                ExcelImportSupport.integer(row, 101 + offset));
    }

    private void importDetailSheets(Workbook workbook) {
        int patentRows = importPatentSheet(workbook.getSheet("2__특허및실용신안"));
        int ntisLeadRows = importNtisLeadSheet(workbook.getSheet("3-1__NTIS(주관)"));
        int ntisCollaborativeRows = importNtisCollaborativeSheet(workbook.getSheet("3-2__NTIS(위탁)"));
        int businessPurposeRows = importBusinessPurposeSheet(workbook.getSheet("4__법인사업목적"));
        log.info(
                "Company template detail sheets imported. patentRows={}, ntisLeadRows={}, ntisCollaborativeRows={}, businessPurposeRows={}",
                patentRows,
                ntisLeadRows,
                ntisCollaborativeRows,
                businessPurposeRows);
    }

    private int importPatentSheet(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        int importedRows = 0;
        for (Row row : sheet) {
            if (row.getRowNum() < 3) {
                continue;
            }
            Integer companyId = ExcelImportSupport.integer(row, 0);
            if (companyId == null) {
                continue;
            }
            String sourceHash = ExcelImportSupport.hash(
                    companyId,
                    ExcelImportSupport.text(row, 1),
                    ExcelImportSupport.text(row, 2),
                    ExcelImportSupport.text(row, 3),
                    ExcelImportSupport.text(row, 4),
                    ExcelImportSupport.text(row, 5),
                    ExcelImportSupport.text(row, 6));
            jdbcTemplate.update(
                    """
                    insert into company_patent (
                        company_id, patent_type, registration_status, application_date,
                        registration_date, company_relation_code, is_active, source_hash
                    ) values (?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (source_hash) do update set
                        patent_type = excluded.patent_type,
                        registration_status = excluded.registration_status,
                        application_date = excluded.application_date,
                        registration_date = excluded.registration_date,
                        company_relation_code = excluded.company_relation_code,
                        is_active = excluded.is_active
                    """,
                    companyId,
                    ExcelImportSupport.text(row, 1),
                    ExcelImportSupport.text(row, 2),
                    ExcelImportSupport.date(row, 3),
                    ExcelImportSupport.date(row, 4),
                    ExcelImportSupport.text(row, 5),
                    ExcelImportSupport.koreanBoolean(row, 6),
                    sourceHash);
            importedRows++;
        }
        return importedRows;
    }

    private int importNtisLeadSheet(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        int importedRows = 0;
        for (Row row : sheet) {
            if (row.getRowNum() < 3) {
                continue;
            }
            Integer companyId = ExcelImportSupport.integer(row, 0);
            Integer referenceYear = ExcelImportSupport.integer(row, 1);
            String projectName = ExcelImportSupport.text(row, 3);
            if (companyId == null || referenceYear == null || projectName == null) {
                continue;
            }
            String sourceHash = ExcelImportSupport.hash(
                    companyId,
                    referenceYear,
                    ExcelImportSupport.text(row, 2),
                    projectName,
                    ExcelImportSupport.text(row, 6),
                    ExcelImportSupport.text(row, 7),
                    ExcelImportSupport.text(row, 8),
                    ExcelImportSupport.text(row, 9));
            jdbcTemplate.update(
                    """
                    insert into company_ntis_lead_project (
                        company_id, reference_year, reference_date, project_name, supervising_ministry_name,
                        region_name, total_research_start_date, total_research_end_date,
                        annual_research_start_date, annual_research_end_date, science_technology_category_name,
                        government_research_fund, private_research_fund, total_research_fund, source_hash
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (source_hash) do update set
                        reference_date = excluded.reference_date,
                        project_name = excluded.project_name,
                        supervising_ministry_name = excluded.supervising_ministry_name,
                        region_name = excluded.region_name,
                        total_research_start_date = excluded.total_research_start_date,
                        total_research_end_date = excluded.total_research_end_date,
                        annual_research_start_date = excluded.annual_research_start_date,
                        annual_research_end_date = excluded.annual_research_end_date,
                        science_technology_category_name = excluded.science_technology_category_name,
                        government_research_fund = excluded.government_research_fund,
                        private_research_fund = excluded.private_research_fund,
                        total_research_fund = excluded.total_research_fund
                    """,
                    companyId,
                    referenceYear,
                    ExcelImportSupport.date(row, 2),
                    projectName,
                    ExcelImportSupport.text(row, 4),
                    ExcelImportSupport.text(row, 5),
                    ExcelImportSupport.date(row, 6),
                    ExcelImportSupport.date(row, 7),
                    ExcelImportSupport.date(row, 8),
                    ExcelImportSupport.date(row, 9),
                    ExcelImportSupport.text(row, 10),
                    ExcelImportSupport.longInteger(row, 11),
                    ExcelImportSupport.longInteger(row, 12),
                    ExcelImportSupport.longInteger(row, 13),
                    sourceHash);
            importedRows++;
        }
        return importedRows;
    }

    private int importNtisCollaborativeSheet(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        int importedRows = 0;
        for (Row row : sheet) {
            if (row.getRowNum() < 3) {
                continue;
            }
            Integer companyId = ExcelImportSupport.integer(row, 0);
            Integer referenceYear = ExcelImportSupport.integer(row, 1);
            if (companyId == null || referenceYear == null) {
                continue;
            }
            String sourceHash = ExcelImportSupport.hash(
                    companyId,
                    referenceYear,
                    ExcelImportSupport.text(row, 2),
                    ExcelImportSupport.text(row, 5),
                    ExcelImportSupport.text(row, 6),
                    ExcelImportSupport.text(row, 7),
                    ExcelImportSupport.text(row, 8),
                    ExcelImportSupport.text(row, 9),
                    ExcelImportSupport.text(row, 10),
                    ExcelImportSupport.text(row, 11));
            jdbcTemplate.update(
                    """
                    insert into company_ntis_collaborative_project (
                        company_id, reference_year, reference_date, has_foreign_institute_collaboration,
                        has_other_collaboration, research_type_name, collaboration_participation_type_name,
                        collaboration_country_name, research_performer_type_name, commissioned_research_fund,
                        collaborative_research_expense, collaborative_research_income,
                        has_company_collaboration, has_university_collaboration,
                        has_public_institute_collaboration, source_hash
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (source_hash) do update set
                        reference_date = excluded.reference_date,
                        has_foreign_institute_collaboration = excluded.has_foreign_institute_collaboration,
                        has_other_collaboration = excluded.has_other_collaboration,
                        research_type_name = excluded.research_type_name,
                        collaboration_participation_type_name = excluded.collaboration_participation_type_name,
                        collaboration_country_name = excluded.collaboration_country_name,
                        research_performer_type_name = excluded.research_performer_type_name,
                        commissioned_research_fund = excluded.commissioned_research_fund,
                        collaborative_research_expense = excluded.collaborative_research_expense,
                        collaborative_research_income = excluded.collaborative_research_income,
                        has_company_collaboration = excluded.has_company_collaboration,
                        has_university_collaboration = excluded.has_university_collaboration,
                        has_public_institute_collaboration = excluded.has_public_institute_collaboration
                    """,
                    companyId,
                    referenceYear,
                    ExcelImportSupport.date(row, 2),
                    ExcelImportSupport.koreanBoolean(row, 3),
                    ExcelImportSupport.koreanBoolean(row, 4),
                    ExcelImportSupport.text(row, 5),
                    ExcelImportSupport.text(row, 6),
                    ExcelImportSupport.text(row, 7),
                    ExcelImportSupport.text(row, 8),
                    ExcelImportSupport.integer(row, 9),
                    ExcelImportSupport.integer(row, 10),
                    ExcelImportSupport.integer(row, 11),
                    ExcelImportSupport.koreanBoolean(row, 12),
                    ExcelImportSupport.koreanBoolean(row, 13),
                    ExcelImportSupport.koreanBoolean(row, 14),
                    sourceHash);
            importedRows++;
        }
        return importedRows;
    }

    private int importBusinessPurposeSheet(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        int importedRows = 0;
        for (Row row : sheet) {
            if (row.getRowNum() < 3) {
                continue;
            }
            Integer companyId = ExcelImportSupport.integer(row, 0);
            Integer displayOrder = ExcelImportSupport.integer(row, 1);
            if (companyId == null || displayOrder == null) {
                continue;
            }
            jdbcTemplate.update(
                    """
                    insert into company_business_purpose (
                        company_id, display_order, business_purpose, registered_date
                    ) values (?, ?, ?, ?)
                    on conflict (company_id, display_order) do update set
                        business_purpose = excluded.business_purpose,
                        registered_date = excluded.registered_date
                    """,
                    companyId,
                    displayOrder,
                    ExcelImportSupport.text(row, 2),
                    ExcelImportSupport.date(row, 3));
            importedRows++;
        }
        return importedRows;
    }

    private Integer establishedYear(Row row, Header header) {
        LocalDate establishedDate = date(row, header.column("설립일자"));
        return establishedDate == null ? null : establishedDate.getYear();
    }

    private String text(Row row, Integer index) {
        return index == null ? null : ExcelImportSupport.text(row, index);
    }

    private Integer integer(Row row, Integer index) {
        return index == null ? null : ExcelImportSupport.integer(row, index);
    }

    private Boolean koreanBoolean(Row row, Integer index) {
        return index == null ? null : ExcelImportSupport.koreanBoolean(row, index);
    }

    private LocalDate date(Row row, Integer index) {
        return index == null ? null : ExcelImportSupport.date(row, index);
    }

    private String normalized(String value) {
        return value == null ? null : value.replaceAll("\\s+", "");
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private record Header(int rowIndex, Map<String, List<Integer>> columns) {
        Integer column(String name) {
            List<Integer> indexes = columns.get(name.replaceAll("\\s+", ""));
            return indexes == null || indexes.isEmpty() ? null : indexes.get(0);
        }

        Integer columnOrDefault(String name, int defaultIndex) {
            Integer index = column(name);
            if (index != null) {
                return index;
            }
            return defaultIndex;
        }
    }

    private record SupportProgramInfo(
            Integer supportYear,
            String code,
            String budgetProgramName,
            String supportType,
            LocalDate startDate,
            LocalDate endDate) {}

    private record CompanyTemplateImportResult(
            int importedRows,
            int createdCompanies,
            int updatedCompanies,
            int supportHistoryRows,
            Set<Integer> companyIds,
            List<String> errors) {

        CompanyTemplateImportResponse toResponse(int metricUpdatedCompanies) {
            return new CompanyTemplateImportResponse(
                    importedRows,
                    createdCompanies,
                    updatedCompanies,
                    supportHistoryRows,
                    metricUpdatedCompanies,
                    errors);
        }
    }
}
