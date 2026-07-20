package site.dataon.hyeyum.dto;

import java.util.List;

public record SupportHistoryPostSupportChangeResponse(
        List<ChangeObservationItem> observations,
        String emptyMessage) {

    public record ChangeObservationItem(
            Integer supportEndYear,
            Integer observationYear,
            int supportCount,
            String titleText,
            String descriptionText,
            List<TopChangeItem> topChanges,
            ObservationStatus observationStatus) {}

    public record TopChangeItem(
            String metric,
            String label,
            Number beforeValue,
            Number afterValue,
            String valueUnit,
            Number changeValue,
            String changeValueUnit,
            Double changeRate,
            String changeRateUnit,
            String displayText) {}

    public record ObservationStatus(String code, String label) {}
}
