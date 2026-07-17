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
    @Column(name = "ntis_lead_project_id", nullable = false)
    private Long ntisLeadProjectId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "reference_year")
    private Integer referenceYear;

    @Column(name = "reference_date")
    private LocalDate referenceDate;

    @Column(name = "project_name", length = 1000)
    private String projectName;

    @Column(name = "supervising_ministry_name", length = 20)
    private String supervisingMinistryName;

    @Column(name = "region_name", length = 1000)
    private String regionName;

    @Column(name = "total_research_start_date")
    private LocalDate totalResearchStartDate;

    @Column(name = "total_research_end_date")
    private LocalDate totalResearchEndDate;

    @Column(name = "annual_research_start_date")
    private LocalDate annualResearchStartDate;

    @Column(name = "annual_research_end_date")
    private LocalDate annualResearchEndDate;

    @Column(name = "science_technology_category_name", length = 1000)
    private String scienceTechnologyCategoryName;

    @Column(name = "government_research_fund")
    private Integer governmentResearchFund;

    @Column(name = "private_research_fund")
    private Integer privateResearchFund;

    @Column(name = "total_research_fund")
    private Integer totalResearchFund;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
