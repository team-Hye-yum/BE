package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "btp_support_program",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_btp_support_program_year_code",
                        columnNames = {"program_year", "code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BtpSupportProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시퀀스//지원사업 내부 식별자")
    @Column(name = "support_program_id", nullable = false)
    private Long supportProgramId;

    @Comment("예시) A1_301")
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Comment("연도// 3개의 시트를 합칠 생각 (예시) 2022")
    @Column(name = "program_year")
    private Integer programYear;

    @Comment("부산TP 예산서의 사업명//예시) 비대면솔루션지원사업")
    @Column(name = "budget_program_name", length = 1000)
    private String budgetProgramName;

    @Comment("사업구분//예시) 기업지원")
    @Column(name = "program_category", length = 20)
    private String programCategory;

    @Comment("사업유형//예시) 패키지지원")
    @Column(name = "support_type", length = 20)
    private String supportType;

    @Comment("시작일//예시) 2022-01-01")
    @Column(name = "start_date")
    private LocalDate startDate;

    @Comment("종료일//예시) 2022-01-02")
    @Column(name = "end_date")
    private LocalDate endDate;

    @Comment("부처명//예시) 산업부")
    @Column(name = "department_name", length = 20)
    private String departmentName;

    @Comment("지자체//예시) 부산시")
    @Column(name = "local_government_name", length = 20)
    private String localGovernmentName;

    @Comment("주요내용//예시) 경제 활성화")
    @Column(name = "program_summary", length = 1000)
    private String programSummary;

    @Comment("기업 마당 공고문 정보//PDF 링크들을 담을 예정")
    @Column(name = "announcement_url", length = 20)
    private String announcementUrl;

    public static BtpSupportProgram create(
            String code,
            Integer programYear,
            String budgetProgramName,
            String programCategory,
            String supportType,
            LocalDate startDate,
            LocalDate endDate,
            String departmentName,
            String localGovernmentName,
            String programSummary) {
        BtpSupportProgram program = new BtpSupportProgram();
        program.code = code;
        program.programYear = programYear;
        program.budgetProgramName = budgetProgramName;
        program.programCategory = programCategory;
        program.supportType = supportType;
        program.startDate = startDate;
        program.endDate = endDate;
        program.departmentName = departmentName;
        program.localGovernmentName = localGovernmentName;
        program.programSummary = programSummary;
        return program;
    }

    public void update(
            String budgetProgramName,
            String programCategory,
            String supportType,
            LocalDate startDate,
            LocalDate endDate,
            String departmentName,
            String localGovernmentName,
            String programSummary) {
        this.budgetProgramName = budgetProgramName;
        this.programCategory = programCategory;
        this.supportType = supportType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.departmentName = departmentName;
        this.localGovernmentName = localGovernmentName;
        this.programSummary = programSummary;
    }
}
