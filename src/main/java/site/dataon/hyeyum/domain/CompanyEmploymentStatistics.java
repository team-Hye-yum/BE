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
        name = "company_employment_statistics",
        indexes = @Index(name = "idx_company_employment_statistics_company_year", columnList = "company_id, year"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyEmploymentStatistics {

    @Id
    @Column(name = "employment_statistics_id", nullable = false)
    private Long employmentStatisticsId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "year")
    private Integer year;

    @Column(name = "employee_counteld3")
    private Integer employeeCount;

    @Column(name = "pension_subscriber_count")
    private Integer pensionSubscriberCount;

    @Column(name = "pension_new_hire_count")
    private Integer pensionNewHireCount;

    @Column(name = "pension_retiree_count")
    private Integer pensionRetireeCount;

    @Column(name = "average_salary")
    private Integer averageSalary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
