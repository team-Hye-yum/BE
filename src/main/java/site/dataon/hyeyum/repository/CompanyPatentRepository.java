package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.dataon.hyeyum.domain.CompanyPatent;

public interface CompanyPatentRepository extends JpaRepository<CompanyPatent, Long> {

    List<CompanyPatent> findByCompanyIdOrderByRegistrationDateDescPatentIdAsc(Integer companyId);

    long countByCompanyIdAndRegistrationStatusAndIsActiveTrue(Integer companyId, String registrationStatus);

    long countByCompanyIdAndPatentTypeAndRegistrationStatusAndIsActiveTrue(
            Integer companyId, String patentType, String registrationStatus);

    @Query(
            """
            select count(p)
            from CompanyPatent p
            where p.companyId = :companyId
              and p.registrationStatus = :registrationStatus
              and p.isActive = true
              and p.registrationDate < :exclusiveEndDate
            """)
    int countRegisteredActivePatentsUntil(
            @Param("companyId") Integer companyId,
            @Param("registrationStatus") String registrationStatus,
            @Param("exclusiveEndDate") java.time.LocalDate exclusiveEndDate);
}
