package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.dataon.hyeyum.domain.CompanyBusinessPurpose;

public interface CompanyBusinessPurposeRepository extends JpaRepository<CompanyBusinessPurpose, Long> {

    List<CompanyBusinessPurpose> findByCompanyIdOrderByDisplayOrderAscBusinessPurposeIdAsc(Integer companyId);
}
