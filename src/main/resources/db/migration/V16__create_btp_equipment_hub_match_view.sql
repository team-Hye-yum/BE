create or replace view public.v_btp_equipment_hub_match as
with coordinate_match as (
    select
        equipment.id as equipment_id,
        nearest.hub_id,
        nearest.distance_km,
        'COORDINATE'::varchar(20) as match_method,
        null::varchar(200) as match_keyword,
        case
            when nearest.distance_km <= 0.2 then 1.00
            when nearest.distance_km <= 0.5 then 0.95
            when nearest.distance_km <= 1.0 then 0.90
            else 0.85
        end::numeric(4, 2) as confidence_score
    from public.btp_equipment equipment
    join lateral (
        select
            hub.hub_id,
            sqrt(
                power((equipment.latitude - hub.latitude) * 111.32, 2)
                + power((equipment.longitude - hub.longitude) * 91.2, 2)
            ) as distance_km
        from public.btp_infra_hub hub
        where equipment.latitude is not null
          and equipment.longitude is not null
          and hub.latitude is not null
          and hub.longitude is not null
          and hub.active = true
        order by distance_km, hub.display_order
        limit 1
    ) nearest on true
    where nearest.distance_km <= 2.0
),
alias_match_candidates as (
    select
        equipment.id as equipment_id,
        alias.hub_id,
        null::numeric as distance_km,
        'ALIAS'::varchar(20) as match_method,
        alias.match_keyword,
        0.70::numeric(4, 2) as confidence_score,
        row_number() over (
            partition by equipment.id
            order by alias.priority desc, length(alias.match_keyword) desc, alias.match_keyword
        ) as match_rank
    from public.btp_equipment equipment
    join public.btp_infra_hub_alias alias
      on equipment.location_name like concat('%', alias.match_keyword, '%')
    join public.btp_infra_hub hub
      on hub.hub_id = alias.hub_id
     and hub.active = true
    where not exists (
        select 1
        from coordinate_match coordinate
        where coordinate.equipment_id = equipment.id
    )
),
alias_match as (
    select
        equipment_id,
        hub_id,
        distance_km,
        match_method,
        match_keyword,
        confidence_score
    from alias_match_candidates
    where match_rank = 1
)
select
    equipment_id,
    hub_id,
    distance_km,
    match_method,
    match_keyword,
    confidence_score
from coordinate_match
union all
select
    equipment_id,
    hub_id,
    distance_km,
    match_method,
    match_keyword,
    confidence_score
from alias_match;

comment on view public.v_btp_equipment_hub_match is
    'Resolved BTP equipment-to-official-hub match. Coordinate nearest hub within 2km wins; alias is fallback.';
