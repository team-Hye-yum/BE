package site.dataon.hyeyum.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record CompanyActivitySupportTimelineResponse(
        TimelineRange timelineRange,
        List<PointEventItem> patentEvents,
        List<TimelineEventItem> ntisEvents,
        List<PointEventItem> btpSupportEvents,
        List<EmptyMessage> emptyMessages) {

    public record TimelineRange(Integer startYear, Integer endYear) {}

    public record PointEventItem(
            String eventType,
            Integer eventYear,
            String eventDate,
            int count,
            String label) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TimelineEventItem(
            String eventType,
            Integer eventYear,
            String eventDate,
            Integer startYear,
            Integer endYear,
            String startDate,
            String endDate,
            int count,
            String label) {}

    public record EmptyMessage(String code, String message) {}
}
