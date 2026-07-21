package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.dataon.hyeyum.domain.CompanyNtisLeadProject;

public interface CompanyNtisLeadProjectRepository extends JpaRepository<CompanyNtisLeadProject, Long> {

    List<CompanyNtisLeadProject> findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisLeadProjectIdAsc(Integer companyId);

    int countByCompanyId(Integer companyId);

    int countByCompanyIdAndReferenceYear(Integer companyId, Integer referenceYear);

    @Query(
            value =
                    """
                    select count(*)::int
                    from (
                        select distinct
                            company_id,
                            project_name,
                            total_research_start_date,
                            total_research_end_date
                        from company_ntis_lead_project
                        where company_id = :companyId
                    ) deduplicated_projects
                    """,
            nativeQuery = true)
    int countDistinctLeadProjectsForScorecard(@Param("companyId") Integer companyId);
}
