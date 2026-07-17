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
        name = "company_employment_statistics",
        indexes = @Index(name = "idx_company_employment_statistics_company_year", columnList = "company_id, year"),
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_company_employment_statistics_company_year",
                        columnNames = {"company_id", "year"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyEmploymentStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시퀀스//이건 자동으로 채번되는 것")
    @Column(name = "employment_statistics_id", nullable = false)
    private Long employmentStatisticsId;

    @Comment("기업일련번호//예시) 117")
    @Column(name = "company_id")
    private Integer companyId;

    @Comment("연도//예시) 2022")
    @Column(name = "year")
    private Integer year;

    @Comment("종업원수//예시) 1")
    @Column(name = "employee_count")
    private Integer employeeCount;

    @Comment("국민연금 가입자수//예시) 1")
    @Column(name = "pension_subscriber_count")
    private Integer pensionSubscriberCount;

    @Comment("국민연금 취업자수//예시) 1")
    @Column(name = "pension_new_hire_count")
    private Integer pensionNewHireCount;

    @Comment("국민연금 퇴직자수//예시) 1")
    @Column(name = "pension_retiree_count")
    private Integer pensionRetireeCount;

    @Comment("1인평균년간급여(합계)_단위:원//예시)  41,123,704")
    @Column(name = "average_salary")
    private Integer averageSalary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
