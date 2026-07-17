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
import org.hibernate.annotations.Comment;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시퀀스//이건 자동으로 채번되는 것")
    @Column(name = "business_purpose_id", nullable = false)
    private Long businessPurposeId;

    @Comment("기업일련번호//예시) 117")
    @Column(name = "company_id")
    private Integer companyId;

    @Comment("순서//예시) 1")
    @Column(name = "display_order")
    private Integer displayOrder;

    @Comment("사업목적항목내용//예시) 1. 환경엔지니어링")
    @Column(name = "business_purpose", length = 1000)
    private String businessPurpose;

    @Comment("등기일자//예시) 20100202")
    @Column(name = "registered_date")
    private LocalDate registeredDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
