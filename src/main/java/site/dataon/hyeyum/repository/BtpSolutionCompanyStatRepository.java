package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import site.dataon.hyeyum.domain.Company;

public interface BtpSolutionCompanyStatRepository extends Repository<Company, Integer> {

    @Query(
            value =
                    """
                    select max(year)
                    from company_employment_statistics
                    """,
            nativeQuery = true)
    Integer findLatestEmploymentYear();

    @Query(
            value =
                    """
                    with btp_companies as (
                        select distinct c.company_id
                        from company c
                        join btp_support_history h on h.company_id = c.company_id
                        where upper(coalesce(c.ksic_code, '')) like concat(:industryPrefix, '%')
                    )
                    select
                        count(b.company_id)::int as establishmentCount,
                        coalesce(sum(e.employee_count), 0)::int as employeeCount
                    from btp_companies b
                    left join company_employment_statistics e
                      on e.company_id = b.company_id
                     and e.year = :year
                    """,
            nativeQuery = true)
    BtpCompanyScaleProjection findBtpScale(@Param("industryPrefix") String industryPrefix, @Param("year") Integer year);

    @Query(
            value =
                    """
                    with btp_companies as (
                        select distinct c.company_id
                        from company c
                        join btp_support_history h on h.company_id = c.company_id
                        where upper(coalesce(c.ksic_code, '')) like concat(:industryPrefix, '%')
                    )
                    select
                        case
                            when e.employee_count between 1 and 4 then '1~4인'
                            when e.employee_count between 5 and 9 then '5~9인'
                            when e.employee_count between 10 and 49 then '10~49인'
                            when e.employee_count between 50 and 299 then '50~299인'
                            when e.employee_count >= 300 then '300인 이상'
                        end as name,
                        count(*)::int as count
                    from btp_companies b
                    join company_employment_statistics e
                      on e.company_id = b.company_id
                     and e.year = :year
                    where e.employee_count is not null
                      and e.employee_count > 0
                    group by name
                    """,
            nativeQuery = true)
    List<BtpCompanyBucketProjection> findBtpEmployeeSizeStats(
            @Param("industryPrefix") String industryPrefix, @Param("year") Integer year);

    @Query(
            value =
                    """
                    with btp_companies as (
                        select distinct c.company_id, c.business_entity_type
                        from company c
                        join btp_support_history h on h.company_id = c.company_id
                        where upper(coalesce(c.ksic_code, '')) like concat(:industryPrefix, '%')
                    )
                    select
                        case
                            when business_entity_type like '%법인%' then 'CORPORATION'
                            when business_entity_type like '%개인%' then 'INDIVIDUAL'
                            else 'UNKNOWN'
                        end as name,
                        count(*)::int as count
                    from btp_companies
                    group by name
                    """,
            nativeQuery = true)
    List<BtpCompanyBucketProjection> findBtpBusinessTypeStats(@Param("industryPrefix") String industryPrefix);
}
