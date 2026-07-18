-- Builds BOK industry revenue indexes from every available revenue growth rate.
-- This is industry-level reference data and does not depend on company rows.
-- The base year is fixed at 2021 = 100. Earlier industry index years cannot be
-- calculated unless the source data provides growth rates before 2022.
begin;

with recursive ranked_growth_rates as (
    select
        source.source_id,
        metric.bok_industry_code,
        metric.year,
        metric.value as growth_rate,
        row_number() over (
            partition by metric.bok_industry_code, metric.year
            order by
                case source.release_version
                    when 'FINAL' then 1
                    when 'PRELIMINARY' then 2
                    else 3
                end,
                source.source_id desc
        ) as row_number
    from industry_benchmark_metric metric
    join industry_benchmark_source source
      on source.source_id = metric.source_id
    where metric.metric = 'REVENUE_GROWTH_RATE'
      and metric.data_status = 'OBSERVED'
      and metric.value is not null
      and metric.year >= 2022
),
growth_rates as (
    select source_id, bok_industry_code, year, growth_rate
    from ranked_growth_rates
    where row_number = 1
),
index_rows as (
    select
        growth_rates.bok_industry_code,
        2021 as year,
        cast(100 as numeric(24, 10)) as index_value,
        cast(null as numeric(24, 10)) as growth_rate,
        growth_rates.source_id,
        'BASE_INDEX' as calculation_type
    from growth_rates
    where growth_rates.year = 2022

    union all

    select
        growth_rates.bok_industry_code,
        growth_rates.year,
        cast(index_rows.index_value * (1 + growth_rates.growth_rate / 100) as numeric(24, 10)) as index_value,
        cast(growth_rates.growth_rate as numeric(24, 10)) as growth_rate,
        growth_rates.source_id,
        'CUMULATIVE_GROWTH' as calculation_type
    from index_rows
    join growth_rates
      on growth_rates.bok_industry_code = index_rows.bok_industry_code
     and growth_rates.year = index_rows.year + 1
)
insert into industry_benchmark_index (
    bok_industry_code,
    metric,
    base_year,
    year,
    index_value,
    growth_rate,
    source_id,
    calculation_type,
    availability_status,
    unavailable_reason
)
select
    bok_industry_code,
    'REVENUE_INDEX',
    2021,
    year,
    index_value,
    growth_rate,
    source_id,
    calculation_type,
    'AVAILABLE',
    null
from index_rows
on conflict (source_id, bok_industry_code, metric, base_year, year) do update set
    index_value = excluded.index_value,
    growth_rate = excluded.growth_rate,
    calculation_type = excluded.calculation_type,
    availability_status = excluded.availability_status,
    unavailable_reason = excluded.unavailable_reason;

commit;
