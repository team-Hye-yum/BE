package site.dataon.hyeyum.service;

final class CompanyAnnualExcelColumns {

    static final int[] YEARS = {2020, 2021, 2022, 2023, 2024};

    private static final int EMPLOYEE_COUNT = 15;
    private static final int PENSION_SUBSCRIBER_COUNT = 20;
    private static final int PENSION_NEW_HIRE_COUNT = 25;
    private static final int PENSION_RETIREE_COUNT = 30;
    private static final int AVERAGE_SALARY = 35;
    private static final int SALES_AMOUNT = 40;
    private static final int OPERATING_INCOME = 45;
    private static final int COST_OF_SALES = 50;
    private static final int NET_INCOME = 55;
    private static final int OPERATING_MARGIN = 60;
    private static final int TOTAL_ASSETS = 65;
    private static final int TOTAL_LIABILITIES = 70;
    private static final int TOTAL_EQUITY = 75;
    private static final int PAID_IN_CAPITAL = 80;
    private static final int RESEARCH_AND_DEVELOPMENT_EXPENSE = 85;
    private static final int REGISTERED_PATENT_COUNT = 96;
    private static final int PATENT_APPLICATION_COUNT = 101;

    private CompanyAnnualExcelColumns() {}

    static int employeeCount(int yearIndex) {
        return EMPLOYEE_COUNT + yearIndex;
    }

    static int pensionSubscriberCount(int yearIndex) {
        return PENSION_SUBSCRIBER_COUNT + yearIndex;
    }

    static int pensionNewHireCount(int yearIndex) {
        return PENSION_NEW_HIRE_COUNT + yearIndex;
    }

    static int pensionRetireeCount(int yearIndex) {
        return PENSION_RETIREE_COUNT + yearIndex;
    }

    static int averageSalary(int yearIndex) {
        return AVERAGE_SALARY + yearIndex;
    }

    static int salesAmount(int yearIndex) {
        return SALES_AMOUNT + yearIndex;
    }

    static int operatingIncome(int yearIndex) {
        return OPERATING_INCOME + yearIndex;
    }

    static int costOfSales(int yearIndex) {
        return COST_OF_SALES + yearIndex;
    }

    static int netIncome(int yearIndex) {
        return NET_INCOME + yearIndex;
    }

    static int operatingMargin(int yearIndex) {
        return OPERATING_MARGIN + yearIndex;
    }

    static int totalAssets(int yearIndex) {
        return TOTAL_ASSETS + yearIndex;
    }

    static int totalLiabilities(int yearIndex) {
        return TOTAL_LIABILITIES + yearIndex;
    }

    static int totalEquity(int yearIndex) {
        return TOTAL_EQUITY + yearIndex;
    }

    static int paidInCapital(int yearIndex) {
        return PAID_IN_CAPITAL + yearIndex;
    }

    static int researchAndDevelopmentExpense(int yearIndex) {
        return RESEARCH_AND_DEVELOPMENT_EXPENSE + yearIndex;
    }

    static int registeredPatentCount(int yearIndex) {
        return REGISTERED_PATENT_COUNT + yearIndex;
    }

    static int patentApplicationCount(int yearIndex) {
        return PATENT_APPLICATION_COUNT + yearIndex;
    }
}
