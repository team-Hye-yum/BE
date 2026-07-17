package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "company")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "region_name", length = 20)
    private String regionName;

    @Column(name = "established_date")
    private LocalDate establishedDate;

    @Column(name = "business_entity_type", length = 20)
    private String businessEntityType;

    @Column(name = "company_size", length = 20)
    private String companySize;

    @Column(name = "listing_status", length = 20)
    private String listingStatus;

    @Column(name = "company_type", length = 20)
    private String companyType;

    @Column(name = "ksic_code", length = 20)
    private String ksicCode;

    @Column(name = "industry_name", length = 1000)
    private String industryName;

    @Column(name = "industry_description", columnDefinition = "text")
    private String industryDescription;

    @Column(name = "main_product", length = 1000)
    private String mainProduct;

    @Column(name = "is_closed")
    private Boolean isClosed;

    @Column(name = "closed_date")
    private LocalDate closedDate;

    @Column(name = "reference_date")
    private LocalDate referenceDate;

    @Column(name = "closure_type", length = 20)
    private String closureType;

    @Column(name = "company_status", length = 20)
    private String companyStatus;

    @Column(name = "is_innobiz")
    private Boolean isInnobiz;

    @Column(name = "is_mainbiz")
    private Boolean isMainbiz;

    @Column(name = "is_venture_company")
    private Boolean isVentureCompany;

    @Column(name = "is_materials_company")
    private Boolean isMaterialsCompany;

    @Column(name = "is_net_certified")
    private Boolean isNetCertified;

    @Column(name = "is_nep_certified")
    private Boolean isNepCertified;

    @Column(name = "researcher_count")
    private Integer researcherCount;

    @Column(name = "has_research_lab")
    private Boolean hasResearchLab;

    @Column(name = "research_lab_registered_date")
    private LocalDate researchLabRegisteredDate;

    @Column(name = "has_rnd_department")
    private Boolean hasRndDepartment;

    @Column(name = "rnd_department_registered_date")
    private LocalDate rndDepartmentRegisteredDate;

    @Column(name = "debt_ratio")
    private Double debtRatio;

    @Column(name = "cost_of_sales_ratio")
    private Double costOfSalesRatio;

    @Column(name = "sales_growth_rate")
    private Double salesGrowthRate;

    @Column(name = "employment_growth_rate")
    private Double employmentGrowthRate;

    @Column(name = "government_rnd_dependency")
    private Double governmentRndDependency;

    @Column(name = "supported_sales_growth_rate")
    private Double supportedSalesGrowthRate;

    @Column(name = "employment_peak_index")
    private Double employmentPeakIndex;

    @Column(name = "employee_turnover_rate")
    private Double employeeTurnoverRate;

    @Column(name = "company_name", length = 20)
    private String companyName;

    @Column(name = "business_registration_number", length = 20)
    private String businessRegistrationNumber;

    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "industry_brief", columnDefinition = "text")
    private String industryBrief;

    @Column(name = "ai_summary", columnDefinition = "text")
    private String aiSummary;

    @Column(name = "ai_one_line_summary", columnDefinition = "text")
    private String aiOneLineSummary;
}
