package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.dataon.hyeyum.domain.CompanyNtisLeadProject;

public interface CompanyNtisLeadProjectRepository extends JpaRepository<CompanyNtisLeadProject, Long> {

    List<CompanyNtisLeadProject> findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisLeadProjectIdAsc(Integer companyId);

    int countByCompanyId(Integer companyId);
}
