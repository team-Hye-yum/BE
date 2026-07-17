package site.dataon.hyeyum.service;

import java.time.LocalDate;

public record SupportProgramAnalysis(
        String code,
        String budgetProgramName,
        String programCategory,
        String supportType,
        LocalDate startDate,
        LocalDate endDate,
        String departmentName,
        String localGovernmentName,
        String programSummary) {}
