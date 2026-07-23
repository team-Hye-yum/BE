package site.dataon.hyeyum.dto;

import java.util.List;

public record BtpSolutionConnectionEvidenceCompaniesResponse(
        String sectionCode,
        Summary summary,
        List<CompanyEvidenceItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public record Summary(long companyCount, long equipmentCount, long hubCount) {}

    public record CompanyEvidenceItem(
            Integer companyId,
            String companyName,
            List<String> mainProducts,
            List<String> connectedFunctions,
            List<ConnectedEquipment> connectedEquipments,
            String evidenceText) {}

    public record ConnectedEquipment(
            Long equipmentId,
            String equipmentName,
            String categoryLarge,
            Long hubId,
            String hubName) {}
}
