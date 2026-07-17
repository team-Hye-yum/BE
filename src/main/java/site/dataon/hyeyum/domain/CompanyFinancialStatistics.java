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
        name = "company_financial_statistics",
        indexes = @Index(name = "idx_company_financial_statistics_company_year", columnList = "company_id, year"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyFinancialStatistics {

    @Id
    @Column(name = "financial_statistics_id", nullable = false)
    private Long financialStatisticsId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "year")
    private Integer year;

    @Column(name = "sales_amount")
    private Integer salesAmount;

    @Column(name = "operating_income")
    private Integer operatingIncome;

    @Column(name = "cost_of_sales")
    private Integer costOfSales;

    @Column(name = "net_income")
    private Integer netIncome;

    @Column(name = "operating_margin")
    private Double operatingMargin;

    @Column(name = "total_assets")
    private Integer totalAssets;

    @Column(name = "total_liabilities")
    private Integer totalLiabilities;

    @Column(name = "total_equity")
    private Integer totalEquity;

    @Column(name = "paid_in_capital")
    private Integer paidInCapital;

    @Column(name = "research_and_development_expense")
    private Integer researchAndDevelopmentExpense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
