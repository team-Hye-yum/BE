package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.dataon.hyeyum.domain.CompanyFinancialStatistics;

public interface CompanyFinancialStatisticsRepository extends JpaRepository<CompanyFinancialStatistics, Long> {

    List<CompanyFinancialStatistics> findByCompanyIdOrderByYearAsc(Integer companyId);
}
