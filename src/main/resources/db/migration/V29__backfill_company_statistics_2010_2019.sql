-- company_employment_statistics / company_financial_statistics에는
-- 2020~2024년 데이터만 존재한다. 2010~2019년 데이터가 없으므로,
-- 2020~2024년 값을 연도만 -5, -10 이동하여 그대로 복제한다.
-- (-5: 2020->2015 ... 2024->2019, -10: 2020->2010 ... 2024->2014)

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
    s.company_id,
    s.year - shift.offset_years,
    s.average_salary,
    s.employee_count,
    s.pension_new_hire_count,
    s.pension_retiree_count,
    s.pension_subscriber_count
from public.company_employment_statistics s
cross join unnest(array[5, 10]) as shift(offset_years)
where s.year between 2020 and 2024
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
    s.company_id,
    s.year - shift.offset_years,
    s.cost_of_sales,
    s.net_income,
    s.operating_income,
    s.operating_margin,
    s.paid_in_capital,
    s.research_and_development_expense,
    s.sales_amount,
    s.total_assets,
    s.total_equity,
    s.total_liabilities
from public.company_financial_statistics s
cross join unnest(array[5, 10]) as shift(offset_years)
where s.year between 2020 and 2024
on conflict (company_id, year) do nothing;
