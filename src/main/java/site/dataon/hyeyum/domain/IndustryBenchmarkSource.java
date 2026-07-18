package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(
        name = "industry_benchmark_source",
        indexes = {
            @Index(name = "idx_industry_benchmark_source_year", columnList = "release_year, observation_year"),
            @Index(name = "idx_industry_benchmark_source_type", columnList = "source_type, release_version")
        },
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_industry_benchmark_source_file_hash",
                        columnNames = "file_hash"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndustryBenchmarkSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Industry benchmark source ID")
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Comment("Source document name, such as BOK 2025 preliminary business analysis")
    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Comment("Source type: BOK_ECOS, BOK_RELEASE_XLSX, BOK_RELEASE_PDF")
    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Comment("Year when the source was released")
    @Column(name = "release_year")
    private Integer releaseYear;

    @Comment("Year measured by the source data")
    @Column(name = "observation_year")
    private Integer observationYear;

    @Comment("Release version: PRELIMINARY or FINAL")
    @Column(name = "release_version", length = 50)
    private String releaseVersion;

    @Comment("Official publication date")
    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Comment("Official source URL")
    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Comment("Hash of the downloaded source file for deduplication")
    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Comment("Survey population, such as audited non-financial corporations")
    @Column(name = "population", length = 200)
    private String population;

    @Comment("Source notes and collection caveats")
    @Column(name = "note", columnDefinition = "text")
    private String note;
}
