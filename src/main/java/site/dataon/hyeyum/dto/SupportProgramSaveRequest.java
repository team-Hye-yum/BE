package site.dataon.hyeyum.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupportProgramSaveRequest(
        @NotBlank String code,
        @NotNull Integer programYear,
        @NotBlank String budgetProgramName,
        String programCategory,
        String supportType,
        @Valid SupportProgramPeriod period,
        String departmentName,
        String localGovernmentName,
        String programSummary) {}
