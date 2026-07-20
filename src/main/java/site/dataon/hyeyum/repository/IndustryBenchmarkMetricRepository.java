package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.dataon.hyeyum.domain.IndustryBenchmarkMetric;

public interface IndustryBenchmarkMetricRepository extends JpaRepository<IndustryBenchmarkMetric, Long> {

    List<IndustryBenchmarkMetric> findByBokIndustryCodeAndMetricOrderByYearAsc(String bokIndustryCode, String metric);
}
