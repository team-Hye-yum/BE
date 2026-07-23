package site.dataon.hyeyum.dto;

import java.util.List;

public record BtpSolutionIndustryOverviewResponse(
        String sectionCode,
        String sectionName,
        Integer busanBaseYear,
        Integer btpBaseYear,
        IndustryScale industryScale,
        BusinessTypeRatio businessTypeRatio,
        List<EmployeeSizeRatio> employeeSizeRatio) {

    public record IndustryScale(CountPair busan, CountPair btp) {}

    public record CountPair(Integer establishmentCount, Integer employeeCount) {}

    public record BusinessTypeRatio(RatioPair busan, RatioPair btp) {}

    public record RatioPair(Double corporationRatio, Double individualRatio) {}

    public record EmployeeSizeRatio(String name, Double busanRatio, Double btpRatio) {}
}
