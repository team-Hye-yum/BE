package site.dataon.hyeyum.dto;

import java.util.List;

public record BtpSolutionInfraConnectionMatrixResponse(List<Item> items) {

    public record Item(
            String divisionCode,
            String divisionName,
            Double employeeGrowthRate,
            Double connectionRate) {}
}
