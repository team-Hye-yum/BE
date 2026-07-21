package site.dataon.hyeyum.service;

import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CompanyDataUpsertService {

    private final JdbcTemplate jdbcTemplate;

    public CompanyDataUpsertService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertEmploymentStatistics(EmploymentStatisticsRow row) {
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
                row.companyId(),
                row.year(),
                row.employeeCount(),
                row.pensionSubscriberCount(),
                row.pensionNewHireCount(),
                row.pensionRetireeCount(),
                row.averageSalary());
    }

    public void upsertFinancialStatistics(FinancialStatisticsRow row) {
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
                row.companyId(),
                row.year(),
                row.salesAmount(),
                row.operatingIncome(),
                row.costOfSales(),
                row.netIncome(),
                row.operatingMargin(),
                row.totalAssets(),
                row.totalLiabilities(),
                row.totalEquity(),
                row.paidInCapital(),
                row.researchAndDevelopmentExpense());
    }

    public void upsertPatentStatistics(PatentStatisticsRow row) {
        jdbcTemplate.update(
                """
                insert into company_patent_statistics (
                    company_id, year, registered_patent_count, patent_application_count
                ) values (?, ?, ?, ?)
                on conflict (company_id, year) do update set
                    registered_patent_count = excluded.registered_patent_count,
                    patent_application_count = excluded.patent_application_count
                """,
                row.companyId(),
                row.year(),
                row.registeredPatentCount(),
                row.patentApplicationCount());
    }

    public void upsertPatent(PatentRow row) {
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
                row.companyId(),
                row.patentType(),
                row.registrationStatus(),
                row.applicationDate(),
                row.registrationDate(),
                row.companyRelationCode(),
                row.isActive(),
                row.sourceHash());
    }

    public void upsertNtisLeadProject(NtisLeadProjectRow row) {
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
                row.companyId(),
                row.referenceYear(),
                row.referenceDate(),
                row.projectName(),
                row.supervisingMinistryName(),
                row.regionName(),
                row.totalResearchStartDate(),
                row.totalResearchEndDate(),
                row.annualResearchStartDate(),
                row.annualResearchEndDate(),
                row.scienceTechnologyCategoryName(),
                row.governmentResearchFund(),
                row.privateResearchFund(),
                row.totalResearchFund(),
                row.sourceHash());
    }

    public void upsertNtisCollaborativeProject(NtisCollaborativeProjectRow row) {
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
                row.companyId(),
                row.referenceYear(),
                row.referenceDate(),
                row.hasForeignInstituteCollaboration(),
                row.hasOtherCollaboration(),
                row.researchTypeName(),
                row.collaborationParticipationTypeName(),
                row.collaborationCountryName(),
                row.researchPerformerTypeName(),
                row.commissionedResearchFund(),
                row.collaborativeResearchExpense(),
                row.collaborativeResearchIncome(),
                row.hasCompanyCollaboration(),
                row.hasUniversityCollaboration(),
                row.hasPublicInstituteCollaboration(),
                row.sourceHash());
    }

    public void upsertBusinessPurpose(BusinessPurposeRow row) {
        jdbcTemplate.update(
                """
                insert into company_business_purpose (
                    company_id, display_order, business_purpose, registered_date
                ) values (?, ?, ?, ?)
                on conflict (company_id, display_order) do update set
                    business_purpose = excluded.business_purpose,
                    registered_date = excluded.registered_date
                """,
                row.companyId(),
                row.displayOrder(),
                row.businessPurpose(),
                row.registeredDate());
    }

    public record EmploymentStatisticsRow(
            Integer companyId,
            Integer year,
            Integer employeeCount,
            Integer pensionSubscriberCount,
            Integer pensionNewHireCount,
            Integer pensionRetireeCount,
            Integer averageSalary) {}

    public record FinancialStatisticsRow(
            Integer companyId,
            Integer year,
            Integer salesAmount,
            Integer operatingIncome,
            Integer costOfSales,
            Integer netIncome,
            Double operatingMargin,
            Integer totalAssets,
            Integer totalLiabilities,
            Integer totalEquity,
            Integer paidInCapital,
            Integer researchAndDevelopmentExpense) {}

    public record PatentStatisticsRow(
            Integer companyId, Integer year, Integer registeredPatentCount, Integer patentApplicationCount) {}

    public record PatentRow(
            Integer companyId,
            String patentType,
            String registrationStatus,
            LocalDate applicationDate,
            LocalDate registrationDate,
            String companyRelationCode,
            Boolean isActive,
            String sourceHash) {}

    public record NtisLeadProjectRow(
            Integer companyId,
            Integer referenceYear,
            LocalDate referenceDate,
            String projectName,
            String supervisingMinistryName,
            String regionName,
            LocalDate totalResearchStartDate,
            LocalDate totalResearchEndDate,
            LocalDate annualResearchStartDate,
            LocalDate annualResearchEndDate,
            String scienceTechnologyCategoryName,
            Long governmentResearchFund,
            Long privateResearchFund,
            Long totalResearchFund,
            String sourceHash) {}

    public record NtisCollaborativeProjectRow(
            Integer companyId,
            Integer referenceYear,
            LocalDate referenceDate,
            Boolean hasForeignInstituteCollaboration,
            Boolean hasOtherCollaboration,
            String researchTypeName,
            String collaborationParticipationTypeName,
            String collaborationCountryName,
            String researchPerformerTypeName,
            Integer commissionedResearchFund,
            Integer collaborativeResearchExpense,
            Integer collaborativeResearchIncome,
            Boolean hasCompanyCollaboration,
            Boolean hasUniversityCollaboration,
            Boolean hasPublicInstituteCollaboration,
            String sourceHash) {}

    public record BusinessPurposeRow(
            Integer companyId, Integer displayOrder, String businessPurpose, LocalDate registeredDate) {}
}
