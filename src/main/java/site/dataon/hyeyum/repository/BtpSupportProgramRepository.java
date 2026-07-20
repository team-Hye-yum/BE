package site.dataon.hyeyum.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.dataon.hyeyum.domain.BtpSupportProgram;

public interface BtpSupportProgramRepository extends JpaRepository<BtpSupportProgram, Long> {

    Optional<BtpSupportProgram> findByProgramYearAndCode(Integer programYear, String code);

    @Query(
            """
            select p
            from BtpSupportProgram p
            where lower(replace(coalesce(p.budgetProgramName, ''), ' ', '')) like lower(concat('%', replace(:keyword, ' ', ''), '%'))
               or lower(coalesce(p.code, '')) like lower(concat('%', :keyword, '%'))
            order by p.programYear desc, p.budgetProgramName, p.code
            """)
    List<BtpSupportProgram> search(@Param("keyword") String keyword);

    @Query(
            value =
                    """
                    with target_companies as (
                        select distinct company_id
                        from btp_support_history
                        where code = :code and support_year = :programYear and company_id is not null
                    )
                    select
                        c.company_id as companyId,
                        c.company_name as companyName,
                        c.business_registration_number as businessRegistrationNumber,
                        c.region_name as regionName,
                        extract(year from c.established_date)::int as establishedYear,
                        c.industry_name as industryName,
                        c.main_product as mainProduct,
                        fs.sales_amount as latestSalesAmount,
                        fs.year as latestSalesYear,
                        es.employee_count as employeeCount,
                        es.year as employeeYear,
                        coalesce(patent_count.registered_patent_count, ps.registered_patent_count, 0) as registeredPatentCount,
                        coalesce(ntis.ntis_project_count, 0) as ntisProjectCount,
                        coalesce(supports.support_count, 0) as supportCount,
                        coalesce(program_history.program_name, :budgetProgramName) as programName,
                        supports.cumulative_support_amount as cumulativeSupportAmount,
                        supports.support_years as supportYears,
                        c.debt_ratio as debtRatio,
                        c.sales_growth_rate as salesGrowthRate
                    from target_companies tc
                    join company c on c.company_id = tc.company_id
                    left join lateral (
                        select year, sales_amount
                        from company_financial_statistics
                        where company_id = c.company_id
                        order by year desc
                        limit 1
                    ) fs on true
                    left join lateral (
                        select year, employee_count
                        from company_employment_statistics
                        where company_id = c.company_id
                        order by year desc
                        limit 1
                    ) es on true
                    left join lateral (
                        select registered_patent_count
                        from company_patent_statistics
                        where company_id = c.company_id
                        order by year desc
                        limit 1
                    ) ps on true
                    left join lateral (
                        select count(*)::int as registered_patent_count
                        from company_patent
                        where company_id = c.company_id and registration_status = '등록' and is_active = true
                    ) patent_count on true
                    left join lateral (
                        select (
                            (select count(*) from company_ntis_lead_project where company_id = c.company_id)
                            + (select count(*) from company_ntis_collaborative_project where company_id = c.company_id)
                        )::int as ntis_project_count
                    ) ntis on true
                    left join lateral (
                        select count(*)::int as support_count,
                               coalesce(sum(support_amount), 0)::int as cumulative_support_amount,
                               string_agg(distinct support_year::text, ',' order by support_year::text) as support_years
                        from btp_support_history
                        where company_id = c.company_id
                    ) supports on true
                    left join lateral (
                        select budget_program_name as program_name
                        from btp_support_history
                        where company_id = c.company_id and code = :code and support_year = :programYear
                        order by support_history_id
                        limit 1
                    ) program_history on true
                    order by c.company_id
                    """,
            nativeQuery = true)
    List<SupportProgramCompanyProjection> findCompaniesForProgram(
            @Param("code") String code,
            @Param("programYear") Integer programYear,
            @Param("budgetProgramName") String budgetProgramName);
}
