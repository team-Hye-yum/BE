package site.dataon.hyeyum.repository;

public interface SupportProgramCompanyProjection {
    Integer getCompanyId();

    String getCompanyName();

    String getBusinessRegistrationNumber();

    String getRegionName();

    Integer getEstablishedYear();

    String getIndustryName();

    String getMainProduct();

    Long getLatestSalesAmount();

    Integer getLatestSalesYear();

    Integer getEmployeeCount();

    Integer getEmployeeYear();

    Integer getRegisteredPatentCount();

    Integer getNtisProjectCount();

    Integer getSupportCount();

    String getProgramName();

    Number getCumulativeSupportAmount();

    String getSupportYears();

    Double getDebtRatio();

    Double getSalesGrowthRate();
}
