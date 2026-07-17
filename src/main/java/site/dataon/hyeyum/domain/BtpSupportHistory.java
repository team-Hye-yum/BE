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
    @Comment("시퀀스//이건 자동으로 채번되는 것")
    @Column(name = "support_history_id", nullable = false)
    private Long supportHistoryId;

    @Comment("연도//3개의 시트를 합칠 생각 (예시) 2022")
    @Column(name = "support_year")
    private Integer supportYear;

    @Comment("코드//예시) B1_311")
    @Column(name = "code", length = 20)
    private String code;

    @Comment("부산TP 예산서의 사업명//예시) 친환경미래에너지마케팅사업")
    @Column(name = "budget_program_name", length = 1000)
    private String budgetProgramName;

    @Comment("사업유형//예시) 사업화지원")
    @Column(name = "support_type", length = 20)
    private String supportType;

    @Comment("지원구분(주요지원)//예시) 전시회")
    @Column(name = "support_category", length = 20)
    private String supportCategory;

    @Comment("지원구분(추가정보)//예시) 마케팅")
    @Column(name = "support_detail", length = 1000)
    private String supportDetail;

    @Comment("지원품목//예시) 베트남Entech, 마케팅조사분석")
    @Column(name = "support_item", length = 1000)
    private String supportItem;

    @Comment("선정일//예시) 2022-08-26")
    @Column(name = "selected_date")
    private LocalDate selectedDate;

    @Comment("선정결과//예시) 지원대상")
    @Column(name = "selection_result", length = 20)
    private String selectionResult;

    @Comment("지원금(천원)//예시)  13,400")
    @Column(name = "support_amount")
    private Integer supportAmount;

    @Comment("시작일//예시) 2022-09-01")
    @Column(name = "start_date")
    private LocalDate startDate;

    @Comment("종료일//예시) 2022-03-31")
    @Column(name = "end_date")
    private LocalDate endDate;

    @Comment("기업일련번호//예시) 117")
    @Column(name = "company_id")
    private Integer companyId;

    @Comment("업종코드//예시) 25929")
    @Column(name = "industry_code", length = 20)
    private String industryCode;

    @Comment("광역//예시) 부산")
    @Column(name = "province_name", length = 20)
    private String provinceName;

    @Comment("기초//예시) 동구")
    @Column(name = "district_name", length = 20)
    private String districtName;

    @Comment("주생산품//예시) 관제장비 및 주변장치")
    @Column(name = "main_product", length = 1000)
    private String mainProduct;

    @Comment("설립연도//예시) 1999")
    @Column(name = "established_year")
    private Integer establishedYear;

    @Column(name = "Field")
    private String field;

    @Comment("원본 데이터 중복 방지를 위한 해시값")
    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
