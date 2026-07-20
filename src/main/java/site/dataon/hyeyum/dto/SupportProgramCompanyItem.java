package site.dataon.hyeyum.dto;

import java.util.List;

public record SupportProgramCompanyItem(
        Integer companyId,
        String companyName,
        String businessRegistrationNumberMasked,
        String region,
        Integer establishedYear,
        String industryName,
        String mainProduct,
        YearlyMoneyAmount latestSalesAmount,
        YearlyCount employeeCount,
        Integer registeredPatentCount,
        Integer ntisProjectCount,
        Integer supportCount,
        String programName,
        MoneyAmount cumulativeSupportAmount,
        List<Integer> supportYears,
        Double debtRatio,
        Double salesGrowthRate) {}
