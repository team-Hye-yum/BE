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
        name = "company_ntis_collaborative_project",
        indexes = @Index(name = "idx_company_ntis_collaborative_project_company_reference_year", columnList = "company_id, reference_year"),
        uniqueConstraints =
                @UniqueConstraint(name = "uk_company_ntis_collaborative_project_source_hash", columnNames = "source_hash"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyNtisCollaborativeProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시퀀스//이건 자동으로 채번되는 것")
    @Column(name = "ntis_collaborative_project_id", nullable = false)
    private Long ntisCollaborativeProjectId;

    @Comment("기업일련번호//예시) 117")
    @Column(name = "company_id")
    private Integer companyId;

    @Comment("기준연도//예시) 2022")
    @Column(name = "reference_year")
    private Integer referenceYear;

    @Comment("기준일자//예시) 20220701")
    @Column(name = "reference_date")
    private LocalDate referenceDate;

    @Comment("외국연구기관공동연구여부//예시) N")
    @Column(name = "has_foreign_institute_collaboration")
    private Boolean hasForeignInstituteCollaboration;

    @Comment("기타공동연구여부//예시) N")
    @Column(name = "has_other_collaboration")
    private Boolean hasOtherCollaboration;

    @Comment("연구형태명//예시) 위탁과제")
    @Column(name = "research_type_name", length = 20)
    private String researchTypeName;

    @Comment("공동연구참여형태명//예시) 기타")
    @Column(name = "collaboration_participation_type_name", length = 20)
    private String collaborationParticipationTypeName;

    @Comment("공동연구참여국가명//예시) 대한민국")
    @Column(name = "collaboration_country_name", length = 20)
    private String collaborationCountryName;

    @Comment("연구수행주체명//예시) 중소기업")
    @Column(name = "research_performer_type_name", length = 20)
    private String researchPerformerTypeName;

    @Comment("위탁과제연구비//예시) 67000000")
    @Column(name = "commissioned_research_fund")
    private Integer commissionedResearchFund;

    @Comment("위탁과제연구비//예시) 80000000")
    @Column(name = "collaborative_research_expense")
    private Integer collaborativeResearchExpense;

    @Comment("공동연구비수입금액//예시) 3000000")
    @Column(name = "collaborative_research_income")
    private Integer collaborativeResearchIncome;

    @Comment("기업공동연구여부//예시) N")
    @Column(name = "has_company_collaboration")
    private Boolean hasCompanyCollaboration;

    @Comment("기업공동연구여부//예시) N")
    @Column(name = "has_university_collaboration")
    private Boolean hasUniversityCollaboration;

    @Comment("기업공동연구여부//예시) N")
    @Column(name = "has_public_institute_collaboration")
    private Boolean hasPublicInstituteCollaboration;

    @Comment("원본 데이터 중복 방지를 위한 해시값")
    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
