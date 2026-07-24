package site.dataon.hyeyum.dto;

public record BtpSolutionInfraConnectionPositionResponse(
        String divisionCode,
        String divisionName,
        Double employeeGrowthRate,
        Double connectionRate,
        Integer baseYear) {}
