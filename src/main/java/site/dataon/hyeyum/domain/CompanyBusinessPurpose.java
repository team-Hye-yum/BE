package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
        name = "company_business_purpose",
        indexes = @Index(name = "idx_company_business_purpose_company_display_order", columnList = "company_id, display_order"),
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_company_business_purpose_company_display_order",
                        columnNames = {"company_id", "display_order"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyBusinessPurpose {

    @Id
    @Column(name = "business_purpose_id", nullable = false)
    private Long businessPurposeId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "business_purpose", length = 1000)
    private String businessPurpose;

    @Column(name = "registered_date")
    private LocalDate registeredDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
