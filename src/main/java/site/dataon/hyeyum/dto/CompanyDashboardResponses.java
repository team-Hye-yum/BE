package site.dataon.hyeyum.dto;

import java.util.List;

public final class CompanyDashboardResponses {

    private CompanyDashboardResponses() {}

    public record CompanyProfileResponse(
            Integer companyId,
            String companyName,
            String region,
            String businessEntityType,
            String companyForm,
            String listingMarket,
            String establishedDate,
            String referenceDate,
            Integer companyAge,
            String ksicCode11th,
            String industryName11th,
            String mainProduct,
            String address) {}

    public record FinancialPositionResponse(Integer companyId, String unit, List<FinancialPositionPoint> series) {}

    public record FinancialPositionPoint(
            Integer year,
            Integer totalAssets,
            Integer totalLiabilities,
            Integer totalEquity,
            Integer paidInCapital,
            DebtRatioDerived derived) {}

    public record DebtRatioDerived(Double debtRatio) {}

    public record IncomeStatementsResponse(Integer companyId, String unit, List<IncomeStatementPoint> series) {}

    public record IncomeStatementPoint(
            Integer year,
            Integer salesAmount,
            Integer costOfSales,
            Integer operatingProfitLoss,
            Integer netIncomeLoss,
            Double operatingProfitMargin,
            Integer researchAndDevelopmentExpense,
            IncomeStatementDerived derived) {}

    public record IncomeStatementDerived(Double salesYoYGrowthRate, Double costOfSalesRatio) {}

    public record EmploymentsResponse(Integer companyId, List<EmploymentPoint> series) {}

    public record EmploymentPoint(
            Integer year,
            Integer employeeCount,
            Integer nationalPensionSubscriberCount,
            Integer nationalPensionJoinerCount,
            Integer nationalPensionLeaverCount,
            Integer nationalPensionAverageAnnualSalary,
            Integer employeeYoYChange,
            Double employeeYoYGrowthRate) {}

    public record CertificationsIpSummaryResponse(
            Integer companyId,
            long activeRegisteredPatentCount,
            List<CertificationBadge> badges,
            List<ResearchOrganization> researchOrganizations) {}

    public record CertificationBadge(String code, String label, Boolean active) {}

    public record ResearchOrganization(String code, String label, Boolean active, String registeredDate) {}

    public record PatentListResponse(List<PatentItem> items) {}

    public record PatentItem(
            Long patentId,
            String patentType,
            String registrationStatus,
            String applicationDate,
            String registrationDate,
            String companyRelationCode,
            Boolean isActive) {}

    public record ProductiveActivitiesSummaryResponse(
            Integer companyId, NtisSummary ntis, SupportHistoriesSummary supportHistories) {}

    public record NtisSummary(
            int leadProjectCount,
            int collaborativeProjectCount,
            MoneyValue recentFiveYearGovernmentResearchFundTotal,
            Integer latestParticipationYear,
            String deduplicationRule) {}

    public record SupportHistoriesSummary(int totalCount, Integer latestSupportYear) {}

    public record NtisLeadProjectListResponse(List<NtisLeadProjectItem> items) {}

    public record NtisLeadProjectItem(
            Long ntisLeadProjectId,
            Integer referenceYear,
            String referenceDate,
            String projectName,
            String supervisingMinistryName,
            String regionName,
            String totalResearchStartDate,
            String totalResearchEndDate,
            String annualResearchStartDate,
            String annualResearchEndDate,
            String scienceTechnologyCategoryName,
            MoneyValue governmentResearchFund,
            MoneyValue privateResearchFund,
            MoneyValue totalResearchFund) {}

    public record NtisCollaborativeProjectListResponse(List<NtisCollaborativeProjectItem> items) {}

    public record NtisCollaborativeProjectItem(
            Long ntisCollaborativeProjectId,
            Integer referenceYear,
            String referenceDate,
            String researchTypeName,
            String collaborationParticipationTypeName,
            String collaborationCountryName,
            String researchPerformerTypeName,
            MoneyValue commissionedResearchFund,
            MoneyValue collaborativeResearchExpense,
            MoneyValue collaborativeResearchIncome,
            Boolean hasCompanyCollaboration,
            Boolean hasUniversityCollaboration,
            Boolean hasPublicInstituteCollaboration,
            Boolean hasForeignInstituteCollaboration,
            Boolean hasOtherCollaboration) {}

    public record ComputedMetricsResponse(Integer companyId, List<ComputedMetricItem> metrics) {}

    public record ComputedMetricItem(String code, String label, Double value, String unit) {}

    public record GrowthScenarioResponse(
            Integer companyId, String disclaimer, List<GrowthChartLine> chartLines, List<SupportMarker> supportMarkers) {}

    public record GrowthChartLine(String code, String label, List<GrowthPoint> points) {}

    public record GrowthPoint(Integer year, Integer value, Double index, String type) {}

    public record SupportMarker(
            Long supportHistoryId,
            String programName,
            String supportItem,
            String startDate,
            String endDate,
            Integer supportYear) {}

    public record AiSummaryResponse(String aiSummary) {}

    public record MoneyValue(Number value, String unit) {}
}
