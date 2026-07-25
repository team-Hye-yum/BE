package site.dataon.hyeyum.dto;

import java.util.List;

public record SupportHistoryCurrentYearStatusResponse(
        Integer year,
        int totalCount,
        int selectedCount,
        List<CurrentYearSupportItem> items,
        String emptyMessage) {

    public record CurrentYearSupportItem(
            Long supportHistoryId,
            String budgetProgramName,
            String supportType,
            String applicationDate,
            String selectionResult) {}
}
