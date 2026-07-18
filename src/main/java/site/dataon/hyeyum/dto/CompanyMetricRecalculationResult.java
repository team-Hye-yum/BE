package site.dataon.hyeyum.dto;

import java.util.List;

public record CompanyMetricRecalculationResult(
        int totalCompanyCount,
        int updatedCompanyCount,
        int aiUpdatedCompanyCount,
        int industryBenchmarkMappingUpdatedCompanyCount,
        int failedCompanyCount,
        List<CompanyMetricError> errors) {}
