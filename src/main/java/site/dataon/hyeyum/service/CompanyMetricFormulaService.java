package site.dataon.hyeyum.service;

import org.springframework.stereotype.Service;

@Service
public class CompanyMetricFormulaService {

    public Double debtRatio(Integer totalLiabilities, Integer totalEquity) {
        return percentage(totalLiabilities, totalEquity);
    }

    public Double costOfSalesRatio(Integer costOfSales, Integer salesAmount) {
        return percentage(costOfSales, salesAmount);
    }

    public Double employmentPeakIndex(Integer employeeCount, Integer pensionSubscriberCount) {
        if (employeeCount == null || pensionSubscriberCount == null || employeeCount == 0) {
            return null;
        }
        return Math.abs(employeeCount - pensionSubscriberCount) / (double) employeeCount * 100.0d;
    }

    public Double employeeTurnoverRate(Integer pensionRetireeCount, Integer pensionSubscriberCount) {
        return percentage(pensionRetireeCount, pensionSubscriberCount);
    }

    public Double governmentRndDependency(Double governmentFund, Double privateFund) {
        double denominator = nullToZero(governmentFund) + nullToZero(privateFund);
        return denominator == 0.0d ? null : nullToZero(governmentFund) / denominator * 100.0d;
    }

    public Double cagr(Double startValue, Double endValue, int yearDiff) {
        if (startValue == null || endValue == null || yearDiff <= 0 || startValue <= 0.0d || endValue <= 0.0d) {
            return null;
        }
        return (Math.pow(endValue / startValue, 1.0d / yearDiff) - 1.0d) * 100.0d;
    }

    private Double percentage(Integer numerator, Integer denominator) {
        Double ratio = ratio(numerator, denominator);
        return ratio == null ? null : ratio * 100.0d;
    }

    private Double ratio(Integer numerator, Integer denominator) {
        if (numerator == null || denominator == null || denominator == 0) {
            return null;
        }
        return numerator / (double) denominator;
    }

    private double nullToZero(Double value) {
        return value == null ? 0.0d : value;
    }
}
