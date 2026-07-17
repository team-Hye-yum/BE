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
        name = "company_patent",
        indexes = @Index(name = "idx_company_patent_company_registration_date", columnList = "company_id, registration_date"),
        uniqueConstraints = @UniqueConstraint(name = "uk_company_patent_source_hash", columnNames = "source_hash"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyPatent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시퀀스//이건 자동으로 채번되는 것")
    @Column(name = "patent_id", nullable = false)
    private Long patentId;

    @Comment("기업일련번호//예시) 117")
    @Column(name = "company_id")
    private Integer companyId;

    @Comment("특허권//예시) 특허권")
    @Column(name = "patent_type", length = 20)
    private String patentType;

    @Comment("등록상태//예시) 등록")
    @Column(name = "registration_status", length = 20)
    private String registrationStatus;

    @Comment("등록상태//예시) 20090220")
    @Column(name = "application_date")
    private LocalDate applicationDate;

    @Comment("등록상태//예시) 20110602")
    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Comment("등록상태//예시) 본인")
    @Column(name = "company_relation_code", length = 20)
    private String companyRelationCode;

    @Comment("등록상태//예시) Y")
    @Column(name = "is_active")
    private Boolean isActive;

    @Comment("원본 데이터 중복 방지를 위한 해시값")
    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
