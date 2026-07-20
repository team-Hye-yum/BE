package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.dataon.hyeyum.domain.CompanyPatent;

public interface CompanyPatentRepository extends JpaRepository<CompanyPatent, Long> {

    List<CompanyPatent> findByCompanyIdOrderByRegistrationDateDescPatentIdAsc(Integer companyId);

    long countByCompanyIdAndRegistrationStatusAndIsActiveTrue(Integer companyId, String registrationStatus);
}
