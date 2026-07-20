package site.dataon.hyeyum.dto;

import java.util.List;
import site.dataon.hyeyum.dto.CompanyDashboardResponses.MoneyValue;

public record SupportHistoryLatestVsPastResponse(
        Summary summary,
        YearlySupportChart yearlySupportChart,
        BtpSupportTimeline btpSupportTimeline,
        List<SupportHistoryCompareItem> latestSupportTargets,
        List<ComparisonItem> comparisons,
        String emptyMessage) {

    public record Summary(
            int btpSelectedSupportCount,
            int latestSupportTargetCount,
            int nationalRndCount,
            int reviewCombinationCount) {}

    public record YearlySupportChart(List<YearlySupportCountItem> items) {}

    public record YearlySupportCountItem(Integer supportYear, String supportType, int supportCount) {}

    public record BtpSupportTimeline(List<BtpSupportTimelineItem> items) {}

    public record BtpSupportTimelineItem(
            Long supportHistoryId,
            Integer supportYear,
            String code,
            String budgetProgramName,
            String supportType,
            String supportCategory,
            String supportDetail,
            String supportItem,
            String selectedDate,
            String selectionResult,
            MoneyValue supportAmount,
            String startDate,
            String endDate) {}

    public record SupportHistoryCompareItem(
            Long supportHistoryId,
            Integer supportYear,
            String budgetProgramName,
            String supportType,
            String supportItem,
            String startDate,
            String endDate,
            MoneyValue supportAmount,
            String selectionResult) {}

    public record ComparisonItem(
            String comparisonId,
            SupportHistoryCompareItem latestSupport,
            SupportHistoryCompareItem pastSupport,
            ReviewSignals reviewSignals,
            String reviewComment) {}

    public record ReviewSignals(
            boolean sameSupportType,
            boolean sameSupportItem,
            boolean supportTypeLinkagePossible,
            boolean continuousSupportWithin12Months) {}
}
