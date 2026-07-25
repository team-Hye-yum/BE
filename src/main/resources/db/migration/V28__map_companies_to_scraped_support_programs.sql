-- 모든 기업(is_closed = false)을 2026년, 2016~2021년 각각에 대해
-- btp_support_program(스크래핑 공고, code 'Z_*') 한 건과 매핑하여 btp_support_history에 적재한다.
-- 매핑 우선순위: 기업의 industry_name에서 추출한 대표 키워드가 해당 연도 공고의
-- budget_program_name/program_category/program_summary에 포함되는 공고를 우선 매칭하고,
-- 일치하는 공고가 없으면 해당 연도 공고 목록에서 company_id 기반으로 결정적으로 하나를 배정한다.
--
-- 성능 메모: btp_support_program은 uk_btp_support_program_year_code UNIQUE(program_year, code)
-- 제약 덕분에 program_year 단독 조회도 그 인덱스를 그대로 탄다. 다만 이 인덱스는 윈도우
-- 함수로 만든 CTE(materialize된 결과, 인덱스 없음)에는 적용되지 않으므로, 아래에서는
-- program_pool CTE 없이 btp_support_program을 LATERAL JOIN으로 직접 조회해 인덱스를 활용한다.

with target_years as (
    select unnest(array[2026, 2021, 2020, 2019, 2018, 2017, 2016]) as support_year
),
year_counts as (
    select program_year, count(*) as program_count
    from public.btp_support_program
    where program_year = any (array[2026, 2021, 2020, 2019, 2018, 2017, 2016])
    group by program_year
),
company_keyword as (
    select
        c.company_id,
        c.ksic_code,
        c.main_product,
        c.region_name,
        c.established_date,
        (
            select token
            from unnest(
                regexp_split_to_array(
                    trim(
                        regexp_replace(
                            regexp_replace(coalesce(c.industry_name, ''), '제조업', '', 'g'),
                            '[,및·]',
                            ' ',
                            'g'
                        )
                    ),
                    '\s+'
                )
            ) with ordinality as t(token, ord)
            where length(token) >= 2
              and token <> '기타'
            order by ord
            limit 1
        ) as keyword
    from public.company c
    where coalesce(c.is_closed, false) = false
),
resolved as (
    select
        ck.company_id,
        ck.ksic_code,
        ck.main_product,
        ck.region_name,
        ck.established_date,
        ty.support_year,
        coalesce(keyword_match.support_program_id, fallback_match.support_program_id) as support_program_id
    from company_keyword ck
    cross join target_years ty
    join year_counts yc on yc.program_year = ty.support_year
    left join lateral (
        select p.support_program_id
        from public.btp_support_program p
        where p.program_year = ty.support_year
          and ck.keyword is not null
          and (
              p.budget_program_name ilike '%' || ck.keyword || '%'
              or p.program_category ilike '%' || ck.keyword || '%'
              or p.program_summary ilike '%' || ck.keyword || '%'
          )
        order by p.code
        limit 1
    ) keyword_match on true
    left join lateral (
        select p.support_program_id
        from public.btp_support_program p
        where keyword_match.support_program_id is null
          and p.program_year = ty.support_year
        order by p.code
        offset (ck.company_id % yc.program_count)
        limit 1
    ) fallback_match on true
)
insert into public.btp_support_history (
    budget_program_name,
    code,
    company_id,
    district_name,
    end_date,
    established_year,
    field,
    industry_code,
    main_product,
    province_name,
    selected_date,
    selection_result,
    source_hash,
    start_date,
    support_amount,
    support_category,
    support_detail,
    support_item,
    support_type,
    support_year
)
select
    p.budget_program_name,
    p.code,
    r.company_id,
    null,
    p.end_date,
    extract(year from r.established_date)::int,
    p.program_category,
    r.ksic_code,
    r.main_product,
    coalesce(r.region_name, p.local_government_name),
    p.start_date,
    '지원대상',
    md5('company:' || r.company_id || ':year:' || r.support_year || ':program:' || p.support_program_id),
    p.start_date,
    null,
    p.program_category,
    p.support_type,
    p.budget_program_name,
    p.support_type,
    r.support_year
from resolved r
join public.btp_support_program p on p.support_program_id = r.support_program_id
where not exists (
    select 1
    from public.btp_support_history h
    where h.company_id = r.company_id
      and h.support_year = r.support_year
)
on conflict (source_hash) do nothing;
