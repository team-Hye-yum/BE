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
        name = "btp_support_history",
        indexes = {
            @Index(name = "idx_btp_support_history_company_year", columnList = "company_id, support_year"),
            @Index(name = "idx_btp_support_history_code", columnList = "code"),
            @Index(name = "idx_btp_support_history_selected_date", columnList = "selected_date")
        },
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_btp_support_history_source_hash",
                        columnNames = "source_hash"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BtpSupportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "support_history_id", nullable = false)
    private Long supportHistoryId;

    @Column(name = "support_year")
    private Integer supportYear;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "budget_program_name", length = 1000)
    private String budgetProgramName;

    @Column(name = "support_type", length = 20)
    private String supportType;

    @Column(name = "support_category", length = 20)
    private String supportCategory;

    @Column(name = "support_detail", length = 1000)
    private String supportDetail;

    @Column(name = "support_item", length = 1000)
    private String supportItem;

    @Column(name = "selected_date")
    private LocalDate selectedDate;

    @Column(name = "selection_result", length = 20)
    private String selectionResult;

    @Column(name = "support_amount")
    private Integer supportAmount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "industry_code", length = 20)
    private String industryCode;

    @Column(name = "province_name", length = 20)
    private String provinceName;

    @Column(name = "district_name", length = 20)
    private String districtName;

    @Column(name = "main_product", length = 1000)
    private String mainProduct;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Column(name = "Field")
    private String field;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
