package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "btp_solution_industry_stat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BtpSolutionIndustryStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stat_id", nullable = false)
    private Long statId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "section_code", nullable = false, length = 10)
    private String sectionCode;

    @Column(name = "section_name", nullable = false, length = 200)
    private String sectionName;

    @Column(name = "stat_category", nullable = false, length = 50)
    private String statCategory;

    @Column(name = "region_name", nullable = false, length = 100)
    private String regionName;

    @Column(name = "middle_industry_name", nullable = false, length = 300)
    private String middleIndustryName;

    @Column(name = "dimension_name", nullable = false, length = 150)
    private String dimensionName;

    @Column(name = "establishment_count")
    private Integer establishmentCount;

    @Column(name = "employee_count")
    private Integer employeeCount;
}
