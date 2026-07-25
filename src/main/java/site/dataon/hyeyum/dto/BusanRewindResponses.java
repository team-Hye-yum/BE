package site.dataon.hyeyum.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class BusanRewindResponses {

    private BusanRewindResponses() {}

    public record CurrentStatus(
            String industryCode,
            String industryName,
            Integer baseYear,
            Integer previousYear,
            CountRatio corporation,
            CountRatio individual,
            Double employeeGrowthRate,
            Integer employeeCount,
            List<EmployeeSizeRatio> employeeSizeRatios,
            List<DistrictEmployeeGrowth> districtEmployeeGrowths) {}

    public record CountRatio(Integer count, Double ratio) {}

    public record EmployeeSizeRatio(String label, Double ratio) {}

    public record DistrictEmployeeGrowth(String sggCode, String districtName, Double growthRate) {}

    public record CurrentSupportPrograms(String industryCode, Integer referenceYear, List<CurrentSupportProgram> items) {}

    public record CurrentSupportProgram(
            Long programId,
            Integer referenceYear,
            String title,
            String status,
            LocalDate dueDate,
            String supportField,
            String summary,
            String announceUrl) {}

    public record TrendBriefing(
            String industryCode,
            String industryName,
            TrendDomestic domestic,
            List<GrowthPoint> growthSeries,
            TrendIssues overseas,
            ChangeComparison changeComparison,
            BusanRelevance busanRelevance,
            String summarySource,
            String aiSummary) {}

    public record TrendDomestic(Double growthRate, List<String> issues) {}

    public record TrendIssues(List<String> issues) {}

    public record GrowthPoint(Integer year, Double growthRate) {}

    public record ChangeComparison(String product, String technology, String demand, String structure) {}

    public record BusanRelevance(List<String> strategicIndustries, List<String> policyKeywords) {}

    public record SimilarFlow(
            String industryCode,
            Period matchedPeriod,
            String flowType,
            String summary,
            List<IndexPoint> series,
            List<PeriodHighlight> periodHighlights) {}

    public record Period(Integer startYear, Integer endYear, String label) {}

    public record IndexPoint(Integer year, Double index) {}

    public record PeriodHighlight(String label, Integer startYear, Integer endYear, Double changeRate) {}

    public record PastSupportReview(
            String industryCode,
            Period matchedPeriod,
            List<IndustryChange> industryChanges,
            String industryChangeSummary,
            List<PastSupportProgram> pastSupportPrograms,
            List<SupportedCompanyChange> supportedCompanyChanges) {}

    public record IndustryChange(String label, Double changeRate) {}

    public record PastSupportProgram(
            Long programId,
            Integer year,
            String title,
            String purpose,
            String target,
            String supportField,
            String supportContent,
            BigDecimal supportAmountThousandKrw) {}

    public record SupportedCompanyChange(
            Integer companyId,
            String companyName,
            Integer supportYear,
            Integer employeeBefore,
            Integer employeeAfter,
            BigDecimal salesBeforeAmount,
            BigDecimal salesAfterAmount,
            String activityChange,
            String rndChange) {}

    public record SupportComparison(
            String industryCode,
            Integer referenceYear,
            List<String> commonFields,
            List<String> pastFields,
            List<ChangedField> changedFields,
            List<String> currentFields,
            List<String> newFields,
            List<String> trendKeywords,
            String aiSummary) {}

    public record AiReviewBriefing(
            String industryCode,
            String industryName,
            String title,
            String briefingMarkdown,
            List<String> briefingLines,
            List<IndustryEvidenceNews> evidenceNews,
            String newsSynthesis,
            String source) {}

    public record IndustryEvidenceNews(
            String publishedAt,
            String industryChange,
            String title,
            String link,
            String source) {}

    public record ChangedField(String from, String to) {}
}
