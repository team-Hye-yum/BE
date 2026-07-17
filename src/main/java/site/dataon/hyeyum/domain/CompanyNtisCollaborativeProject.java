package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "company_ntis_collaborative_project",
        indexes = @Index(name = "idx_company_ntis_collaborative_project_company_reference_year", columnList = "company_id, reference_year"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyNtisCollaborativeProject {

    @Id
    @Column(name = "ntis_collaborative_project_id", nullable = false)
    private Long ntisCollaborativeProjectId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "reference_year")
    private Integer referenceYear;

    @Column(name = "reference_date")
    private LocalDate referenceDate;

    @Column(name = "has_foreign_institute_collaboration")
    private Boolean hasForeignInstituteCollaboration;

    @Column(name = "has_other_collaboration")
    private Boolean hasOtherCollaboration;

    @Column(name = "research_type_name", length = 20)
    private String researchTypeName;

    @Column(name = "collaboration_participation_type_name", length = 20)
    private String collaborationParticipationTypeName;

    @Column(name = "collaboration_country_name", length = 20)
    private String collaborationCountryName;

    @Column(name = "research_performer_type_name", length = 20)
    private String researchPerformerTypeName;

    @Column(name = "commissioned_research_fund")
    private Integer commissionedResearchFund;

    @Column(name = "collaborative_research_expense")
    private Integer collaborativeResearchExpense;

    @Column(name = "collaborative_research_income")
    private Integer collaborativeResearchIncome;

    @Column(name = "has_company_collaboration")
    private Boolean hasCompanyCollaboration;

    @Column(name = "has_university_collaboration")
    private Boolean hasUniversityCollaboration;

    @Column(name = "has_public_institute_collaboration")
    private Boolean hasPublicInstituteCollaboration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
