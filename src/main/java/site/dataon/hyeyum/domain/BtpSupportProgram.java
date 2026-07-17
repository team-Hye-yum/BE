package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "btp_support_program")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BtpSupportProgram {

    @Id
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
