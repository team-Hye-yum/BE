package site.dataon.hyeyum.dto;

public record SupportProgramSearchItem(
        String code,
        Integer programYear,
        String budgetProgramName,
        String programCategory,
        String supportType,
        SupportProgramPeriod period,
        String departmentName,
        String localGovernmentName) {}
