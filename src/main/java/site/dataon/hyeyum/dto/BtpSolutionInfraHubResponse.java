package site.dataon.hyeyum.dto;

import java.math.BigDecimal;
import java.util.List;

public record BtpSolutionInfraHubResponse(String sectionCode, List<InfraHub> hubs) {

    public record InfraHub(
            Long hubId,
            String hubName,
            String hubKind,
            String centerName,
            String summary,
            String address,
            String districtName,
            String tel,
            BigDecimal latitude,
            BigDecimal longitude,
            String imageUrl,
            String spaceUrl,
            String directionsUrl,
            Integer equipmentCount,
            List<CategoryCount> topEquipmentCategories,
            List<SampleEquipment> sampleEquipments,
            List<Facility> facilities) {}

    public record CategoryCount(String name, Integer count) {}

    public record SampleEquipment(Long equipmentId, String equipmentName, String categoryLarge, String locationName) {}

    public record Facility(
            Long facilityId,
            String siteName,
            String buildingNo,
            String buildingName,
            String grossFloorArea,
            String floors,
            String purpose) {}
}
