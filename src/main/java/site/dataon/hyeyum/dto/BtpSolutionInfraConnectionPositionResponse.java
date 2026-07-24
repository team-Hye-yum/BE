package site.dataon.hyeyum.dto;

public record BtpSolutionInfraConnectionPositionResponse(
        String divisionCode,
        String divisionName,
        Double employeeGrowthRate,
        Double connectionRate,
        String comparisonZone,
        String statisticalBasis,
        Integer btpSupportedCompanyCount,
        Integer baseYear) {}
