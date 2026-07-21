package site.dataon.hyeyum.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.dto.DataImportResult;
import site.dataon.hyeyum.service.CompanyDataUpsertService.BusinessPurposeRow;
import site.dataon.hyeyum.service.CompanyDataUpsertService.EmploymentStatisticsRow;
import site.dataon.hyeyum.service.CompanyDataUpsertService.FinancialStatisticsRow;
import site.dataon.hyeyum.service.CompanyDataUpsertService.NtisCollaborativeProjectRow;
import site.dataon.hyeyum.service.CompanyDataUpsertService.NtisLeadProjectRow;
import site.dataon.hyeyum.service.CompanyDataUpsertService.PatentRow;
import site.dataon.hyeyum.service.CompanyDataUpsertService.PatentStatisticsRow;

@Service
public class KodataExcelImportService {

    private final JdbcTemplate jdbcTemplate;
    private final CompanyDataUpsertService companyDataUpsertService;

    public KodataExcelImportService(JdbcTemplate jdbcTemplate, CompanyDataUpsertService companyDataUpsertService) {
        this.jdbcTemplate = jdbcTemplate;
        this.companyDataUpsertService = companyDataUpsertService;
    }

    @Transactional
    public DataImportResult importFile(MultipartFile file) {
        DataImportCounter counter = new DataImportCounter();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            ensureNtisLeadFundColumnsAreBigint();
            importCompanySheet(workbook.getSheetAt(0), counter);
            importPatentSheet(workbook.getSheetAt(1), counter);
            importNtisLeadSheet(workbook.getSheetAt(2), counter);
            importNtisCollaborativeSheet(workbook.getSheetAt(3), counter);
            importBusinessPurposeSheet(workbook.getSheetAt(4), counter);
        } catch (IOException exception) {
            throw new IllegalArgumentException("KODATA 엑셀 파일을 읽을 수 없습니다.", exception);
        }
        return new DataImportResult(file.getOriginalFilename(), counter.snapshot());
    }

    private void ensureNtisLeadFundColumnsAreBigint() {
        jdbcTemplate.execute(
                """
                alter table if exists company_ntis_lead_project
                    alter column government_research_fund type bigint,
                    alter column private_research_fund type bigint,
                    alter column total_research_fund type bigint
                """);
    }

    private void importCompanySheet(org.apache.poi.ss.usermodel.Sheet sheet, DataImportCounter counter) {
        for (Iterator<Row> iterator = sheet.rowIterator(); iterator.hasNext(); ) {
            Row row = iterator.next();
            if (row.getRowNum() < 6) {
                continue;
            }
            Integer companyId = ExcelImportSupport.integer(row, 0);
            if (companyId == null) {
                continue;
            }
            upsertCompany(row, companyId);
            counter.increment("company");
            upsertAnnualStatistics(row, companyId, counter);
        }
    }

    private void upsertCompany(Row row, Integer companyId) {
        jdbcTemplate.update(
                """
                insert into company (
                    company_id, region_name, established_date, business_entity_type, company_size,
                    listing_status, company_type, ksic_code, industry_name, main_product,
                    is_closed, closed_date, reference_date, closure_type, company_status,
                    is_innobiz, is_mainbiz, is_venture_company, is_materials_company,
                    is_net_certified, is_nep_certified, researcher_count, has_research_lab,
                    research_lab_registered_date, has_rnd_department, rnd_department_registered_date
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (company_id) do update set
                    region_name = excluded.region_name,
                    established_date = excluded.established_date,
                    business_entity_type = excluded.business_entity_type,
                    company_size = excluded.company_size,
                    listing_status = excluded.listing_status,
                    company_type = excluded.company_type,
                    ksic_code = excluded.ksic_code,
                    industry_name = excluded.industry_name,
                    main_product = excluded.main_product,
                    is_closed = excluded.is_closed,
                    closed_date = excluded.closed_date,
                    reference_date = excluded.reference_date,
                    closure_type = excluded.closure_type,
                    company_status = excluded.company_status,
                    is_innobiz = excluded.is_innobiz,
                    is_mainbiz = excluded.is_mainbiz,
                    is_venture_company = excluded.is_venture_company,
                    is_materials_company = excluded.is_materials_company,
                    is_net_certified = excluded.is_net_certified,
                    is_nep_certified = excluded.is_nep_certified,
                    researcher_count = excluded.researcher_count,
                    has_research_lab = excluded.has_research_lab,
                    research_lab_registered_date = excluded.research_lab_registered_date,
                    has_rnd_department = excluded.has_rnd_department,
                    rnd_department_registered_date = excluded.rnd_department_registered_date
                """,
                companyId,
                ExcelImportSupport.text(row, 1),
                ExcelImportSupport.date(row, 2),
                ExcelImportSupport.text(row, 3),
                ExcelImportSupport.text(row, 4),
                ExcelImportSupport.text(row, 5),
                ExcelImportSupport.text(row, 6),
                ExcelImportSupport.text(row, 7),
                ExcelImportSupport.text(row, 8),
                ExcelImportSupport.text(row, 9),
                ExcelImportSupport.koreanBoolean(row, 10),
                ExcelImportSupport.date(row, 11),
                ExcelImportSupport.date(row, 12),
                ExcelImportSupport.text(row, 13),
                ExcelImportSupport.text(row, 14),
                ExcelImportSupport.koreanBoolean(row, 90),
                ExcelImportSupport.koreanBoolean(row, 91),
                ExcelImportSupport.koreanBoolean(row, 92),
                ExcelImportSupport.koreanBoolean(row, 93),
                ExcelImportSupport.koreanBoolean(row, 94),
                ExcelImportSupport.koreanBoolean(row, 95),
                ExcelImportSupport.integer(row, 106),
                ExcelImportSupport.koreanBoolean(row, 107),
                ExcelImportSupport.date(row, 108),
                ExcelImportSupport.koreanBoolean(row, 109),
                ExcelImportSupport.date(row, 110));
    }

    private void upsertAnnualStatistics(Row row, Integer companyId, DataImportCounter counter) {
        for (int yearIndex = 0; yearIndex < CompanyAnnualExcelColumns.YEARS.length; yearIndex++) {
            int year = CompanyAnnualExcelColumns.YEARS[yearIndex];
            upsertEmployment(row, companyId, year, yearIndex);
            upsertFinancial(row, companyId, year, yearIndex);
            upsertPatentStatistics(row, companyId, year, yearIndex);
            counter.increment("company_employment_statistics");
            counter.increment("company_financial_statistics");
            counter.increment("company_patent_statistics");
        }
    }

    private void upsertEmployment(Row row, Integer companyId, int year, int yearIndex) {
        companyDataUpsertService.upsertEmploymentStatistics(new EmploymentStatisticsRow(
                companyId,
                year,
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.employeeCount(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.pensionSubscriberCount(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.pensionNewHireCount(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.pensionRetireeCount(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.averageSalary(yearIndex))));
    }

    private void upsertFinancial(Row row, Integer companyId, int year, int yearIndex) {
        companyDataUpsertService.upsertFinancialStatistics(new FinancialStatisticsRow(
                companyId,
                year,
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.salesAmount(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.operatingIncome(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.costOfSales(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.netIncome(yearIndex)),
                ExcelImportSupport.decimal(row, CompanyAnnualExcelColumns.operatingMargin(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.totalAssets(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.totalLiabilities(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.totalEquity(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.paidInCapital(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.researchAndDevelopmentExpense(yearIndex))));
    }

    private void upsertPatentStatistics(Row row, Integer companyId, int year, int yearIndex) {
        companyDataUpsertService.upsertPatentStatistics(new PatentStatisticsRow(
                companyId,
                year,
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.registeredPatentCount(yearIndex)),
                ExcelImportSupport.integer(row, CompanyAnnualExcelColumns.patentApplicationCount(yearIndex))));
    }

    private void importPatentSheet(org.apache.poi.ss.usermodel.Sheet sheet, DataImportCounter counter) {
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
            companyDataUpsertService.upsertPatent(new PatentRow(
                    companyId,
                    ExcelImportSupport.text(row, 1),
                    ExcelImportSupport.text(row, 2),
                    ExcelImportSupport.date(row, 3),
                    ExcelImportSupport.date(row, 4),
                    ExcelImportSupport.text(row, 5),
                    ExcelImportSupport.koreanBoolean(row, 6),
                    sourceHash));
            counter.increment("company_patent");
        }
    }

    private void importNtisLeadSheet(org.apache.poi.ss.usermodel.Sheet sheet, DataImportCounter counter) {
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
            String sourceHash = ExcelImportSupport.hash(companyId, referenceYear, ExcelImportSupport.text(row, 2), projectName,
                    ExcelImportSupport.text(row, 6), ExcelImportSupport.text(row, 7), ExcelImportSupport.text(row, 8),
                    ExcelImportSupport.text(row, 9));
            companyDataUpsertService.upsertNtisLeadProject(new NtisLeadProjectRow(
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
                    sourceHash));
            counter.increment("company_ntis_lead_project");
        }
    }

    private void importNtisCollaborativeSheet(org.apache.poi.ss.usermodel.Sheet sheet, DataImportCounter counter) {
        for (Row row : sheet) {
            if (row.getRowNum() < 3) {
                continue;
            }
            Integer companyId = ExcelImportSupport.integer(row, 0);
            Integer referenceYear = ExcelImportSupport.integer(row, 1);
            if (companyId == null || referenceYear == null) {
                continue;
            }
            String sourceHash = ExcelImportSupport.hash(companyId, referenceYear, ExcelImportSupport.text(row, 2),
                    ExcelImportSupport.text(row, 5), ExcelImportSupport.text(row, 6), ExcelImportSupport.text(row, 7),
                    ExcelImportSupport.text(row, 8), ExcelImportSupport.text(row, 9), ExcelImportSupport.text(row, 10),
                    ExcelImportSupport.text(row, 11));
            companyDataUpsertService.upsertNtisCollaborativeProject(new NtisCollaborativeProjectRow(
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
                    sourceHash));
            counter.increment("company_ntis_collaborative_project");
        }
    }

    private void importBusinessPurposeSheet(org.apache.poi.ss.usermodel.Sheet sheet, DataImportCounter counter) {
        for (Row row : sheet) {
            if (row.getRowNum() < 3) {
                continue;
            }
            Integer companyId = ExcelImportSupport.integer(row, 0);
            Integer displayOrder = ExcelImportSupport.integer(row, 1);
            if (companyId == null || displayOrder == null) {
                continue;
            }
            companyDataUpsertService.upsertBusinessPurpose(new BusinessPurposeRow(
                    companyId,
                    displayOrder,
                    ExcelImportSupport.text(row, 2),
                    ExcelImportSupport.date(row, 3)));
            counter.increment("company_business_purpose");
        }
    }
}
