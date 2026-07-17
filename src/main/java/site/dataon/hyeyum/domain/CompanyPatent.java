package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "company_patent",
        indexes = @Index(name = "idx_company_patent_company_registration_date", columnList = "company_id, registration_date"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyPatent {

    @Id
    @Column(name = "patent_id", nullable = false)
    private Long patentId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "patent_type", length = 20)
    private String patentType;

    @Column(name = "registration_status", length = 20)
    private String registrationStatus;

    @Column(name = "application_date")
    private LocalDate applicationDate;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "company_relation_code", length = 20)
    private String companyRelationCode;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
