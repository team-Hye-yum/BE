package site.dataon.hyeyum.dto;

import java.util.List;

public record CompanyMetricRecalculationResult(
        int totalCompanyCount,
        int updatedCompanyCount,
        int failedCompanyCount,
        List<CompanyMetricError> errors) {}
