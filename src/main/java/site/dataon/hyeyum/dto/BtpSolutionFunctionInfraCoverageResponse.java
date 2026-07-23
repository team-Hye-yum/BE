package site.dataon.hyeyum.dto;

public record BtpSolutionFunctionInfraCoverageResponse(
        String divisionCode,
        String divisionName,
        int detectedFunctionCount,
        int connectedFunctionCount,
        int unconnectedFunctionCount,
        Double coverageRate) {}
