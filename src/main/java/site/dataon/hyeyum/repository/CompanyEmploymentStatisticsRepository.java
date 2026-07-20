package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import site.dataon.hyeyum.domain.CompanyEmploymentStatistics;

public interface CompanyEmploymentStatisticsRepository extends JpaRepository<CompanyEmploymentStatistics, Long> {

    List<CompanyEmploymentStatistics> findByCompanyIdOrderByYearAsc(Integer companyId);

    @Query("select max(s.year) from CompanyEmploymentStatistics s")
    Integer findMaxYear();
}
