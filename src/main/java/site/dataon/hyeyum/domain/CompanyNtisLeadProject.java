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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(
        name = "company_ntis_lead_project",
        indexes = @Index(name = "idx_company_ntis_lead_project_company_reference_year", columnList = "company_id, reference_year"),
        uniqueConstraints = @UniqueConstraint(name = "uk_company_ntis_lead_project_source_hash", columnNames = "source_hash"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyNtisLeadProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시퀀스//이건 자동으로 채번되는 것")
    @Column(name = "ntis_lead_project_id", nullable = false)
    private Long ntisLeadProjectId;

    @Comment("기업일련번호//예시) 117")
    @Column(name = "company_id")
    private Integer companyId;

    @Comment("기준연도//예시) 2022")
    @Column(name = "reference_year")
    private Integer referenceYear;

    @Comment("기준일자//예시) 20230101")
    @Column(name = "reference_date")
    private LocalDate referenceDate;

    @Comment("NTIS사업명//예시) 농업실용화기술R&D지원")
    @Column(name = "project_name", length = 1000)
    private String projectName;

    @Comment("주관부처명//예시) 농촌진흥청")
    @Column(name = "supervising_ministry_name", length = 20)
    private String supervisingMinistryName;

    @Comment("지역구분명//예시) 부산광역시 강서구")
    @Column(name = "region_name", length = 1000)
    private String regionName;

    @Comment("총연구시작일//예시) 20220201")
    @Column(name = "total_research_start_date")
    private LocalDate totalResearchStartDate;

    @Comment("총연구종료일/예시) 20221130")
    @Column(name = "total_research_end_date")
    private LocalDate totalResearchEndDate;

    @Comment("총연구종료일//예시) 20220201")
    @Column(name = "annual_research_start_date")
    private LocalDate annualResearchStartDate;

    @Comment("당해연구종료일//예시) 20221130")
    @Column(name = "annual_research_end_date")
    private LocalDate annualResearchEndDate;

    @Comment("과학기술표준분류명//예시) 농업 환경정화")
    @Column(name = "science_technology_category_name", length = 1000)
    private String scienceTechnologyCategoryName;

    @Comment("정부투자연구비//예시)  85,500,000")
    @Column(name = "government_research_fund")
    private Long governmentResearchFund;

    @Comment("민간연구비합계//예시)  11,125,000")
    @Column(name = "private_research_fund")
    private Long privateResearchFund;

    @Comment("연구비합계//예시)  122,250,000")
    @Column(name = "total_research_fund")
    private Long totalResearchFund;

    @Comment("원본 데이터 중복 방지를 위한 해시값")
    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
