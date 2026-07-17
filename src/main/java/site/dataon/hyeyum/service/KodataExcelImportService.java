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

@Service
public class KodataExcelImportService {

    private static final int[] YEARS = {2020, 2021, 2022, 2023, 2024};

    private final JdbcTemplate jdbcTemplate;

    public KodataExcelImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public DataImportResult importFile(MultipartFile file) {
        DataImportCounter counter = new DataImportCounter();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
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
        for (int i = 0; i < YEARS.length; i++) {
            int year = YEARS[i];
            upsertEmployment(row, companyId, year, i);
            upsertFinancial(row, companyId, year, i);
            upsertPatentStatistics(row, companyId, year, i);
            counter.increment("company_employment_statistics");
            counter.increment("company_financial_statistics");
            counter.increment("company_patent_statistics");
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
                    ExcelImportSupport.integer(row, 11),
                    ExcelImportSupport.integer(row, 12),
                    ExcelImportSupport.integer(row, 13),
                    sourceHash);
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
            counter.increment("company_business_purpose");
        }
    }
}
