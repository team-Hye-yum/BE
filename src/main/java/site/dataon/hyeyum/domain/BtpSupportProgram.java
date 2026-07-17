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
    @Column(name = "support_program_id", nullable = false)
    private Long supportProgramId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "program_year")
    private Integer programYear;

    @Column(name = "budget_program_name", length = 1000)
    private String budgetProgramName;

    @Column(name = "program_category", length = 20)
    private String programCategory;

    @Column(name = "support_type", length = 20)
    private String supportType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "department_name", length = 20)
    private String departmentName;

    @Column(name = "local_government_name", length = 20)
    private String localGovernmentName;

    @Column(name = "program_summary", length = 1000)
    private String programSummary;

    @Column(name = "announcement_url", length = 20)
    private String announcementUrl;
}
