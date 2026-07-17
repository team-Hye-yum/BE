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

@Getter
@Entity
@Table(
        name = "company_patent",
        indexes = @Index(name = "idx_company_patent_company_registration_date", columnList = "company_id, registration_date"),
        uniqueConstraints = @UniqueConstraint(name = "uk_company_patent_source_hash", columnNames = "source_hash"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyPatent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
