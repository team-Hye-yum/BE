package site.dataon.hyeyum.dto;

import java.util.List;

public record BtpSolutionConnectionEvidenceSummaryResponse(List<Item> items) {

    public record Item(
            String type,
            String label,
            Integer count,
            String description) {}
}
