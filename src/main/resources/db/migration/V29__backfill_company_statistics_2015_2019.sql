-- company_employment_statistics / company_financial_statistics에는
-- 2020~2024년 데이터만 존재한다. 2015~2019년 데이터가 없으므로,
-- 2020~2024년 값을 연도만 -5 이동(2020->2015 ... 2024->2019)하여 그대로 복제한다.

insert into public.company_employment_statistics (
    company_id,
    year,
    average_salary,
    employee_count,
    pension_new_hire_count,
    pension_retiree_count,
    pension_subscriber_count
)
select
    company_id,
    year - 5,
    average_salary,
    employee_count,
    pension_new_hire_count,
    pension_retiree_count,
    pension_subscriber_count
from public.company_employment_statistics
where year between 2020 and 2024
on conflict (company_id, year) do nothing;

insert into public.company_financial_statistics (
    company_id,
    year,
    cost_of_sales,
    net_income,
    operating_income,
    operating_margin,
    paid_in_capital,
    research_and_development_expense,
    sales_amount,
    total_assets,
    total_equity,
    total_liabilities
)
select
    company_id,
    year - 5,
    cost_of_sales,
    net_income,
    operating_income,
    operating_margin,
    paid_in_capital,
    research_and_development_expense,
    sales_amount,
    total_assets,
    total_equity,
    total_liabilities
from public.company_financial_statistics
where year between 2020 and 2024
on conflict (company_id, year) do nothing;
