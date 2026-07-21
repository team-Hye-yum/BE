package site.dataon.hyeyum.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupportProgramSaveRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,20}$") String code,
        @NotNull @Min(2000) @Max(2100) Integer programYear,
        @NotBlank @Size(max = 1000) String budgetProgramName,
        @Size(max = 20) String programCategory,
        @Size(max = 20) String supportType,
        @Valid SupportProgramPeriod period,
        @Size(max = 20) String departmentName,
        @Size(max = 20) String localGovernmentName,
        @Size(max = 1000) String programSummary) {}
