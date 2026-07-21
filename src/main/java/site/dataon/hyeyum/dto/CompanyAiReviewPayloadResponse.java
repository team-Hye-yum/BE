package site.dataon.hyeyum.dto;

import java.util.List;

public record CompanyAiReviewPayloadResponse(
        Integer companyId,
        CompanyProfile profile,
        SupportSummary supportSummary,
        EmploymentSummary employmentSummary,
        ReviewOptions options) {

    public record CompanyProfile(
            String companyName,
            String industryName,
            String industryDescription,
            String industryBrief,
            String ksicCode,
            String mainProduct,
            String regionName,
            Double supportedSalesGrowthRate,
            String companyActivityText,
            List<String> businessPurposes,
            List<String> ntisProjectNames,
            PatentSummary patentSummary) {}

    public record PatentSummary(Long activeRegisteredPatentCount, Integer latestRegistrationYear) {}

    public record SupportSummary(
            Integer totalSupportCount,
            Integer marketExpansionSupportCount,
            Integer techRnDSupportCount,
            Boolean jobCreationSupportSelected,
            List<String> recentSupportTexts) {}

    public record EmploymentSummary(
            Integer observationYear,
            Integer employeeCountPreviousYear,
            Integer employeeCountObservationYear,
            Integer pensionSubscriberCount,
            Integer pensionNewHireCount,
            Integer pensionRetireeCount,
            Double employeeTurnoverRate,
            String turnoverRatePeriod,
            Double turnoverBenchmarkRate,
            String benchmarkPeriod,
            String benchmarkSource) {}

    public record ReviewOptions(Integer maxEvidenceCount) {}
}
