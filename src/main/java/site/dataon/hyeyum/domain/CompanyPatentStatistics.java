package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "company_patent_statistics",
        indexes = @Index(name = "idx_company_patent_statistics_company_year", columnList = "company_id, year"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyPatentStatistics {

    @Id
    @Column(name = "patent_statistics_id", nullable = false)
    private Long patentStatisticsId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "year")
    private Integer year;

    @Column(name = "registered_patent_count")
    private Integer registeredPatentCount;

    @Column(name = "patent_application_count")
    private Integer patentApplicationCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
