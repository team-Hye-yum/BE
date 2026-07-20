package site.dataon.hyeyum.dto;

public record SupportProgramAnalysisPayload(
        Integer programYear,
        String budgetProgramName,
        String programCategory,
        String supportType,
        SupportProgramPeriod period,
        String departmentName,
        String localGovernmentName,
        String programSummary) {}
