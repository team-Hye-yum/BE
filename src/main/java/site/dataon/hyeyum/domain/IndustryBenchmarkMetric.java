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
        name = "industry_benchmark_metric",
        indexes = {
            @Index(name = "idx_industry_benchmark_metric_industry", columnList = "bok_industry_code, year"),
            @Index(name = "idx_industry_benchmark_metric_metric", columnList = "metric, year"),
            @Index(name = "idx_industry_benchmark_metric_source", columnList = "source_id")
        },
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_industry_benchmark_metric_source_industry_metric_year",
                        columnNames = {"source_id", "bok_industry_code", "metric", "year"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndustryBenchmarkMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Industry benchmark metric ID")
    @Column(name = "benchmark_metric_id", nullable = false)
    private Long benchmarkMetricId;

    @Comment("Source document ID")
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Comment("BOK industry code, such as C29, C31, J, ALL")
    @Column(name = "bok_industry_code", nullable = false, length = 50)
    private String bokIndustryCode;

    @Comment("BOK industry name")
    @Column(name = "bok_industry_name", length = 200)
    private String bokIndustryName;

    @Comment("Metric code: REVENUE_GROWTH_RATE, OPERATING_MARGIN")
    @Column(name = "metric", nullable = false, length = 50)
    private String metric;

    @Comment("Observation year")
    @Column(name = "year", nullable = false)
    private Integer year;

    @Comment("Metric value as published or parsed")
    @Column(name = "value", precision = 24, scale = 10)
    private BigDecimal value;

    @Comment("Unit: %, %P, INDEX")
    @Column(name = "unit", length = 20)
    private String unit;

    @Comment("Value type: LEVEL, GROWTH_RATE, REFERENCE")
    @Column(name = "value_type", length = 50)
    private String valueType;

    @Comment("Release version copied from the source")
    @Column(name = "release_version", length = 50)
    private String releaseVersion;

    @Comment("Data status: OBSERVED, MISSING, NOT_PUBLISHED")
    @Column(name = "data_status", length = 50)
    private String dataStatus;

    @Comment("Raw label from the source sheet or API item")
    @Column(name = "raw_label", length = 500)
    private String rawLabel;
}
