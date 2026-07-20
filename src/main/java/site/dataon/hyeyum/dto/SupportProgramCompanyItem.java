package site.dataon.hyeyum.dto;

public record SupportProgramCompanyItem(
        Integer companyId,
        String companyName,
        String region,
        Integer establishedYear,
        String industryName,
        String mainProduct,
        YearlyMoneyAmount latestSalesAmount,
        Integer latestEmployeeCount,
        Integer registeredPatentCount,
        Integer ntisProjectCount,
        Integer supportCount,
        MoneyAmount cumulativeSupportAmount,
        Double debtRatio,
        Double salesGrowthRate) {}
