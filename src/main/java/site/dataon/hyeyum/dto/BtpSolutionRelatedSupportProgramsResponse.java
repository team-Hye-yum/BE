package site.dataon.hyeyum.dto;

import java.util.List;

public record BtpSolutionRelatedSupportProgramsResponse(List<Item> items) {

    public record Item(
            Long programId,
            String title,
            Integer year,
            String status,
            String supportField,
            String supportContent,
            String connectionBasis,
            String linkedEquipment,
            String sourceUrl) {}
}
