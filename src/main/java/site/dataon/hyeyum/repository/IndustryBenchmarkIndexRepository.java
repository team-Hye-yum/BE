package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.dataon.hyeyum.domain.IndustryBenchmarkIndex;

public interface IndustryBenchmarkIndexRepository extends JpaRepository<IndustryBenchmarkIndex, Long> {

    List<IndustryBenchmarkIndex> findByBokIndustryCodeAndMetricAndBaseYearOrderByYearAsc(
            String bokIndustryCode, String metric, Integer baseYear);
}
