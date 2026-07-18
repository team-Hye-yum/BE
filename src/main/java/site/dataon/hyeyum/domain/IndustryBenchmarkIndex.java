package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(
        name = "industry_benchmark_index",
        indexes = {
            @Index(name = "idx_industry_benchmark_index_industry", columnList = "bok_industry_code, year"),
            @Index(name = "idx_industry_benchmark_index_metric", columnList = "metric, base_year"),
            @Index(name = "idx_industry_benchmark_index_source", columnList = "source_id")
        },
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_industry_benchmark_index_source_industry_metric_base_year",
                        columnNames = {"source_id", "bok_industry_code", "metric", "base_year", "year"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndustryBenchmarkIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Industry benchmark index ID")
    @Column(name = "industry_index_id", nullable = false)
    private Long industryIndexId;

    @Comment("BOK industry code")
    @Column(name = "bok_industry_code", nullable = false, length = 50)
    private String bokIndustryCode;

    @Comment("Metric represented by the index, such as REVENUE")
    @Column(name = "metric", nullable = false, length = 50)
    private String metric;

    @Comment("Base year for index calculation")
    @Column(name = "base_year", nullable = false)
    private Integer baseYear;

    @Comment("Observation year")
    @Column(name = "year", nullable = false)
    private Integer year;

    @Comment("Index value, commonly 2021 = 100")
    @Column(name = "index_value", precision = 24, scale = 10)
    private BigDecimal indexValue;

    @Comment("Growth rate used to derive this index value")
    @Column(name = "growth_rate", precision = 24, scale = 10)
    private BigDecimal growthRate;

    @Comment("Source document ID")
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Comment("Calculation type: BASE_INDEX, CUMULATIVE_GROWTH")
    @Column(name = "calculation_type", length = 50)
    private String calculationType;

    @Comment("Availability status")
    @Column(name = "availability_status", length = 50)
    private String availabilityStatus;

    @Comment("Reason when index is unavailable")
    @Column(name = "unavailable_reason", length = 100)
    private String unavailableReason;
}
