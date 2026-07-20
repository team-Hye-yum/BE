package site.dataon.hyeyum.dto;

import java.util.List;

public record CompanyTemplateImportResponse(
        int importedRows,
        int createdCompanies,
        int updatedCompanies,
        int supportHistoryRows,
        int metricUpdatedCompanies,
        List<String> errors) {}
