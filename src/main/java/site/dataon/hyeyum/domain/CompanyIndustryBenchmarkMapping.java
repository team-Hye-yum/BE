package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(
        name = "company_industry_benchmark_mapping",
        indexes = {
            @Index(name = "idx_company_industry_benchmark_mapping_ksic", columnList = "ksic_code"),
            @Index(name = "idx_company_industry_benchmark_mapping_bok", columnList = "bok_industry_code"),
            @Index(name = "idx_company_industry_benchmark_mapping_status", columnList = "mapping_status")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyIndustryBenchmarkMapping {

    @Id
    @Comment("Company ID")
    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Comment("KSIC code copied from company")
    @Column(name = "ksic_code", length = 20)
    private String ksicCode;

    @Comment("Mapped BOK industry code")
    @Column(name = "bok_industry_code", length = 50)
    private String bokIndustryCode;

    @Comment("Mapping status")
    @Column(name = "mapping_status", length = 50)
    private String mappingStatus;

    @Comment("Mapping level")
    @Column(name = "mapping_level", length = 50)
    private String mappingLevel;

    @Comment("Whether revenue benchmark can be shown")
    @Column(name = "usable_for_revenue_benchmark")
    private Boolean usableForRevenueBenchmark;

    @Comment("Whether operating margin benchmark can be shown")
    @Column(name = "usable_for_operating_margin_benchmark")
    private Boolean usableForOperatingMarginBenchmark;

    @Comment("Reason when benchmark mapping cannot be used")
    @Column(name = "unavailable_reason", length = 100)
    private String unavailableReason;

    @Comment("Last update timestamp")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
