package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import site.dataon.hyeyum.domain.CompanyFinancialStatistics;

public interface CompanyFinancialStatisticsRepository extends JpaRepository<CompanyFinancialStatistics, Long> {

    List<CompanyFinancialStatistics> findByCompanyIdOrderByYearAsc(Integer companyId);

    List<CompanyFinancialStatistics> findTop2ByCompanyIdOrderByYearDesc(Integer companyId);

    @Query("select max(s.year) from CompanyFinancialStatistics s")
    Integer findMaxYear();
}
