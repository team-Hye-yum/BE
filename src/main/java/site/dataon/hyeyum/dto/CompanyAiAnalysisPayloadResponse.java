package site.dataon.hyeyum.dto;

import java.time.LocalDate;
import java.util.List;

public record CompanyAiAnalysisPayloadResponse(
        Integer companyId,
        Profile profile,
        Capabilities capabilities,
        Financials financials,
        Employment employment,
        SupportHistory supportHistory,
        Options options) {

    public record Profile(
            String industryName,
            String industryBrief,
            String ksicCode,
            String regionName,
            LocalDate establishedDate,
            String companySize,
            String mainProduct) {}

    public record Capabilities(
            List<String> businessPurposes,
            List<String> ntisProjectNames,
            Integer ntisProjectCount,
            PatentSummary patentSummary,
            ResearchOrganizations researchOrganizations) {}

    public record PatentSummary(Long activeRegisteredPatentCount, Integer latestRegistrationYear) {}

    public record ResearchOrganizations(
            Boolean hasResearchLab,
            Boolean hasRndDepartment,
            Integer researcherCount) {}

    public record Financials(
            Integer latestYear,
            Integer latestSalesAmount,
            Double salesGrowthRate,
            Double supportedSalesGrowthRate,
            Double debtRatio,
            Double governmentRndDependency,
            Integer latestRndExpense) {}

    public record Employment(
            Integer observationYear,
            Integer employeeCountPreviousYear,
            Integer employeeCountObservationYear,
            Integer pensionSubscriberCount,
            Integer pensionNewHireCount,
            Integer pensionRetireeCount,
            Double employeeTurnoverRate) {}

    public record SupportHistory(
            Integer totalSupportCount,
            Integer marketExpansionSupportCount,
            Integer techRnDSupportCount,
            Boolean jobCreationSupportSelected,
            List<String> recentSupportTexts) {}

    public record Options(Integer lineCount) {}
}
