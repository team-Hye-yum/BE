package site.dataon.hyeyum.dto;

import java.util.List;

public final class CompanyScorecardResponses {

    private CompanyScorecardResponses() {}

    public record CompanyScorecardSummaryResponse(
            FinancialCard financial,
            EmploymentCard employment,
            CertificationsIpCard certificationsIp,
            ResearchActivityCard researchActivity) {}

    public record FinancialCard(
            DisplayMoneyValue salesAmount,
            Integer salesYear,
            DisplayNumericValue salesYoYGrowthRate,
            DisplayNumericValue operatingMargin) {}

    public record EmploymentCard(
            Integer employeeCount,
            Integer employeeYear,
            Integer employeeYoYChange,
            DisplayNumericValue employeeYoYGrowthRate) {}

    public record CertificationsIpCard(long activeRegisteredPatentCount, List<BadgeItem> certificationBadges) {}

    public record ResearchActivityCard(
            int ntisLeadProjectCount,
            String ntisDisplayText,
            BusanAxDxBadge busanAxDx) {}

    public record BusanAxDxBadge(boolean badgeVisible, int evidenceCount, String displayText) {}

    public record BusanAxDxEvidenceResponse(
            boolean badgeVisible,
            int evidenceCount,
            List<EvidenceGroup> evidenceGroups,
            String emptyMessage) {}

    public record EvidenceGroup(String evidenceType, String label, List<EvidenceItem> items) {}

    public record EvidenceItem(
            String sourceType,
            String matchedKeyword,
            String originalText,
            Integer supportYear,
            String selectionResult,
            String displayText) {}

    public record DisplayMoneyValue(Number value, String displayText) {}

    public record DisplayNumericValue(Number value, String displayText) {}

    public record BadgeItem(String code, String label) {}
}
