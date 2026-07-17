package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "company")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @Comment("기업일련번호//예시) 117")
    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Comment("지역//예시) 부산")
    @Column(name = "region_name", length = 20)
    private String regionName;

    @Comment("설립일자//예시) 20100202")
    @Column(name = "established_date")
    private LocalDate establishedDate;

    @Comment("기업유형(법입/개인)//예시) 법인기업")
    @Column(name = "business_entity_type", length = 20)
    private String businessEntityType;

    @Comment("기업유형(법입/개인)//예시) 소상공인")
    @Column(name = "company_size", length = 20)
    private String companySize;

    @Comment("기업공개(코스피,코스닥)//예시) 일반법인")
    @Column(name = "listing_status", length = 20)
    private String listingStatus;

    @Comment("기업형태 (주식/개인)//예시) 주식회사")
    @Column(name = "company_type", length = 20)
    private String companyType;

    @Comment("KSIC코드 (11차)//예시) C21309")
    @Column(name = "ksic_code", length = 20)
    private String ksicCode;

    @Comment("업종명 (11차)//예시) 그 외 기타 의료용품 및 의약 관련제품 제조업")
    @Column(name = "industry_name", length = 1000)
    private String industryName;

    @Comment("업종명 (11차) 상세정보//보민님이 찾은 정보를 넣을 생각 (예시)")
    @Column(name = "industry_description", columnDefinition = "text")
    private String industryDescription;

    @Comment("주요제품//예시) 골이식재_덴탈")
    @Column(name = "main_product", length = 1000)
    private String mainProduct;

    @Comment("휴폐업여부//예시) N")
    @Column(name = "is_closed")
    private Boolean isClosed;

    @Comment("폐업일자//예시)")
    @Column(name = "closed_date")
    private LocalDate closedDate;

    @Comment("조회일자//예시) 20260630")
    @Column(name = "reference_date")
    private LocalDate referenceDate;

    @Comment("휴폐업구분//예시) 부가가치세 일반과세자")
    @Column(name = "closure_type", length = 20)
    private String closureType;

    @Comment("기업상태//예시) 정상")
    @Column(name = "company_status", length = 20)
    private String companyStatus;

    @Comment("이노비즈 여부//예시) 유")
    @Column(name = "is_innobiz")
    private Boolean isInnobiz;

    @Comment("메인비즈 여부//예시) 유")
    @Column(name = "is_mainbiz")
    private Boolean isMainbiz;

    @Comment("벤처기업 여부//예시) 유")
    @Column(name = "is_venture_company")
    private Boolean isVentureCompany;

    @Comment("소재부품 여부//예시) 유")
    @Column(name = "is_materials_company")
    private Boolean isMaterialsCompany;

    @Comment("NET 여부//예시) 유")
    @Column(name = "is_net_certified")
    private Boolean isNetCertified;

    @Comment("NEP 여부//예시) 유")
    @Column(name = "is_nep_certified")
    private Boolean isNepCertified;

    @Comment("최근 연구원수//예시) 3")
    @Column(name = "researcher_count")
    private Integer researcherCount;

    @Comment("기업부설연구소 유무//예시) 여")
    @Column(name = "has_research_lab")
    private Boolean hasResearchLab;

    @Comment("기업부설연구소 등록일//예시) 20161221")
    @Column(name = "research_lab_registered_date")
    private LocalDate researchLabRegisteredDate;

    @Comment("연구개발전담부서 유무//예시) 부")
    @Column(name = "has_rnd_department")
    private Boolean hasRndDepartment;

    @Comment("연구개발전담부서 등록일//예시) 20201231")
    @Column(name = "rnd_department_registered_date")
    private LocalDate rndDepartmentRegisteredDate;

    @Comment("부채비율/계산식을 적용해서 미리 넣어둘 생각 (예시)")
    @Column(name = "debt_ratio")
    private Double debtRatio;

    @Comment("매출원가율//계산식을 적용해서 미리 넣어둘 생각 (예시)")
    @Column(name = "cost_of_sales_ratio")
    private Double costOfSalesRatio;

    @Comment("매출 성장성//계산식을 적용해서 미리 넣어둘 생각 (예시)")
    @Column(name = "sales_growth_rate")
    private Double salesGrowthRate;

    @Comment("고용 성장성//계산식을 적용해서 미리 넣어둘 생각 (예시)")
    @Column(name = "employment_growth_rate")
    private Double employmentGrowthRate;

    @Comment("정부 R&D 의존도//계산식을 적용해서 미리 넣어둘 생각(없을 수 있음) (예시)")
    @Column(name = "government_rnd_dependency")
    private Double governmentRndDependency;

    @Comment("지원 기업 매출 변화율/계산식을 적용해서 미리 넣어둘 생각 (예시)")
    @Column(name = "supported_sales_growth_rate")
    private Double supportedSalesGrowthRate;

    @Comment("고용 괴리지수//계산식을 적용해서 미리 넣어둘 생각 (예시)")
    @Column(name = "employment_peak_index")
    private Double employmentPeakIndex;

    @Comment("고용 회전율//계산식을 적용해서 미리 넣어둘 생각 (예시)")
    @Column(name = "employee_turnover_rate")
    private Double employeeTurnoverRate;

    @Comment("기업명//우리가 만들어야함 (예시)")
    @Column(name = "company_name", length = 20)
    private String companyName;

    @Comment("사업자등록번호//우리가 만들어야함 (예시)")
    @Column(name = "business_registration_number", length = 20)
    private String businessRegistrationNumber;

    @Comment("위치//우리가 가공해야 함 (예시)")
    @Column(name = "address", length = 1000)
    private String address;

    @Comment("업종 맥락 브리핑//AI로 미리 저장해둘 것 (예시)")
    @Column(name = "industry_brief", columnDefinition = "text")
    private String industryBrief;

    @Comment("AI 3줄 요약//AI로 미리 저장해둘 것 (예시)")
    @Column(name = "ai_summary", columnDefinition = "text")
    private String aiSummary;

    @Comment("AI 한줄 요약//AI로 미리 저장해둘 것 (예시)")
    @Column(name = "ai_one_line_summary", columnDefinition = "text")
    private String aiOneLineSummary;
}
