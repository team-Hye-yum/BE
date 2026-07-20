package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.dataon.hyeyum.domain.CompanyNtisCollaborativeProject;

public interface CompanyNtisCollaborativeProjectRepository extends JpaRepository<CompanyNtisCollaborativeProject, Long> {

    List<CompanyNtisCollaborativeProject> findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisCollaborativeProjectIdAsc(
            Integer companyId);

    int countByCompanyId(Integer companyId);

    int countByCompanyIdAndReferenceYear(Integer companyId, Integer referenceYear);
}
