package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(
        name = "ksic_bok_industry_mapping",
        indexes = {
            @Index(name = "idx_ksic_bok_industry_mapping_ksic", columnList = "ksic_code"),
            @Index(name = "idx_ksic_bok_industry_mapping_bok", columnList = "bok_industry_code"),
            @Index(name = "idx_ksic_bok_industry_mapping_status", columnList = "mapping_status")
        },
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_ksic_bok_industry_mapping_ksic",
                        columnNames = "ksic_code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KsicBokIndustryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("KSIC to BOK industry mapping ID")
    @Column(name = "mapping_id", nullable = false)
    private Long mappingId;

    @Comment("KSIC code from KODATA, such as C29199")
    @Column(name = "ksic_code", nullable = false, length = 20)
    private String ksicCode;

    @Comment("KSIC section code, such as C")
    @Column(name = "ksic_section_code", length = 10)
    private String ksicSectionCode;

    @Comment("KSIC division code, such as 29")
    @Column(name = "ksic_division_code", length = 10)
    private String ksicDivisionCode;

    @Comment("KSIC industry name")
    @Column(name = "ksic_name", length = 500)
    private String ksicName;

    @Comment("Mapped BOK industry code, such as C29")
    @Column(name = "bok_industry_code", length = 50)
    private String bokIndustryCode;

    @Comment("Mapped BOK industry name")
    @Column(name = "bok_industry_name", length = 200)
    private String bokIndustryName;

    @Comment("Mapping level: EXACT, DIVISION, SECTION, PROXY, UNAVAILABLE")
    @Column(name = "mapping_level", length = 50)
    private String mappingLevel;

    @Comment("Mapping status: CONFIRMED, PENDING_REVIEW, AMBIGUOUS, UNAVAILABLE")
    @Column(name = "mapping_status", length = 50)
    private String mappingStatus;

    @Comment("Reason for confidence level or ambiguity")
    @Column(name = "confidence_reason", columnDefinition = "text")
    private String confidenceReason;

    @Comment("Reviewer identifier")
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Comment("Review timestamp")
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
