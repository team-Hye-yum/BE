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
        name = "company_financial_statistics",
        indexes = @Index(name = "idx_company_financial_statistics_company_year", columnList = "company_id, year"),
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_company_financial_statistics_company_year",
                        columnNames = {"company_id", "year"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyFinancialStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시퀀스//이건 자동으로 채번되는 것")
    @Column(name = "financial_statistics_id", nullable = false)
    private Long financialStatisticsId;

    @Comment("기업일련번호//예시) 117")
    @Column(name = "company_id")
    private Integer companyId;

    @Comment("연도//예시) 2022")
    @Column(name = "year")
    private Integer year;

    @Comment("매출액//예시)  28,018,841")
    @Column(name = "sales_amount")
    private Integer salesAmount;

    @Comment("영업이익손실//예시) -540,086")
    @Column(name = "operating_income")
    private Integer operatingIncome;

    @Comment("매출원가//예시)  24,399,948")
    @Column(name = "cost_of_sales")
    private Integer costOfSales;

    @Comment("당기순이익손실//예시) -2,820,077")
    @Column(name = "net_income")
    private Integer netIncome;

    @Comment("영업이익률//예시) -53.17")
    @Column(name = "operating_margin")
    private Double operatingMargin;

    @Comment("자산총계//예시)  4,281,095")
    @Column(name = "total_assets")
    private Integer totalAssets;

    @Comment("부채총계//예시)  8,355,775")
    @Column(name = "total_liabilities")
    private Integer totalLiabilities;

    @Comment("자본총계//예시)  205,024")
    @Column(name = "total_equity")
    private Integer totalEquity;

    @Comment("납입자본금//예시)  950,000")
    @Column(name = "paid_in_capital")
    private Integer paidInCapital;

    @Comment("연구개발비//예시)  773,482")
    @Column(name = "research_and_development_expense")
    private Integer researchAndDevelopmentExpense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
