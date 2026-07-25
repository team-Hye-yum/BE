-- Fill only missing display-critical company metrics from the nearest non-null
-- value for the same company. This keeps observed values unchanged while
-- preventing Busan Rewind supported-company cards from showing "-명" / "-".

update public.company_employment_statistics target
set employee_count = (
    select source.employee_count
    from public.company_employment_statistics source
    where source.company_id = target.company_id
      and source.employee_count is not null
    order by abs(source.year - target.year), source.year desc
    limit 1
)
where target.employee_count is null
  and exists (
      select 1
      from public.company_employment_statistics source
      where source.company_id = target.company_id
        and source.employee_count is not null
  );

update public.company_financial_statistics target
set sales_amount = (
    select source.sales_amount
    from public.company_financial_statistics source
    where source.company_id = target.company_id
      and source.sales_amount is not null
    order by abs(source.year - target.year), source.year desc
    limit 1
)
where target.sales_amount is null
  and exists (
      select 1
      from public.company_financial_statistics source
      where source.company_id = target.company_id
        and source.sales_amount is not null
  );
