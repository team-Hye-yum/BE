package site.dataon.hyeyum.dto;

import java.util.List;

public record BtpSolutionRelatedSupportProgramEquipmentsResponse(
        Long programId,
        List<Item> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public record Item(
            Long equipmentId,
            String equipmentName,
            String hubName,
            String categoryLarge,
            String categoryMiddle) {}
}
