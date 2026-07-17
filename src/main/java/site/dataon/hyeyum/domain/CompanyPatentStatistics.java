package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(
        name = "company_patent_statistics",
        indexes = @Index(name = "idx_company_patent_statistics_company_year", columnList = "company_id, year"),
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_company_patent_statistics_company_year",
                        columnNames = {"company_id", "year"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyPatentStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시퀀스//이건 자동으로 채번되는 것")
    @Column(name = "patent_statistics_id", nullable = false)
    private Long patentStatisticsId;

    @Comment("기업일련번호//예시) 117")
    @Column(name = "company_id")
    private Integer companyId;

    @Comment("연도//예시) 2022")
    @Column(name = "year")
    private Integer year;

    @Comment("특허등록건수//예시) 3")
    @Column(name = "registered_patent_count")
    private Integer registeredPatentCount;

    @Comment("특허출원건수//예시) 1")
    @Column(name = "patent_application_count")
    private Integer patentApplicationCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
