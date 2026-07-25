-- btp_support_program은 2016~2018년(100%)과 2019년 일부 행에서 원본 스크래핑 시점에
-- program_category/support_type이 채워지지 않아 비어 있다. 이 상태로는 "현재 지원 사업과 비교"
-- 기능이 과거 구간의 분야를 전혀 인식하지 못해 모든 현재 분야가 "신규"로 잘못 표시된다.
--
-- 2020~2026년 데이터에서 실제로 쓰인 카테고리 어휘를 기준으로, budget_program_name/program_summary
-- 텍스트에서 키워드를 찾아 역추론해 채운다. 여러 키워드가 매칭되면 콤마로 묶어서 기존 데이터의
-- 복합 카테고리 표기 방식(예: '컨설팅,마케팅,기타')과 동일한 형식으로 저장하고,
-- 아무 키워드도 매칭되지 않으면 기존 데이터에서 가장 흔한 '기타'로 채운다.

with keyword_defs(keyword, category) as (
    values
        ('시제품', '시제품'),
        ('시작품', '시제품'),
        ('컨설팅', '컨설팅'),
        ('고급화', '제품고급화'),
        ('디자인', '디자인'),
        ('마케팅', '마케팅'),
        ('전시회', '전시회'),
        ('박람회', '전시회'),
        ('시험인증', '시험인증'),
        ('인증', '시험인증'),
        ('인력양성', '인력양성'),
        ('일자리', '인력양성'),
        ('채용', '인력양성'),
        ('R&D', 'RnD'),
        ('연구개발', 'RnD'),
        ('기반구축', '기반구축'),
        ('패키지', '패키지지원'),
        ('기업지원', '기업지원')
),
blank_programs as (
    select
        p.support_program_id,
        p.budget_program_name || ' ' || coalesce(p.program_summary, '') as haystack
    from public.btp_support_program p
    where coalesce(p.support_type, '') = ''
      and coalesce(p.program_category, '') = ''
),
matched as (
    select
        bp.support_program_id,
        array_agg(distinct kd.category order by kd.category) as categories
    from blank_programs bp
    join keyword_defs kd on bp.haystack ilike '%' || kd.keyword || '%'
        -- "(비R&D)"처럼 R&D를 부정하는 표기가 흔해서, 그 경우엔 RnD 매칭을 제외한다.
        and not (
            kd.category = 'RnD'
            and (bp.haystack ilike '%비R&D%' or bp.haystack ilike '%비연구개발%')
        )
    group by bp.support_program_id
)
update public.btp_support_program p
set
    support_type = coalesce(array_to_string(m.categories, ','), '기타'),
    program_category = coalesce(array_to_string(m.categories, ','), '기타')
from blank_programs bp
left join matched m on m.support_program_id = bp.support_program_id
where p.support_program_id = bp.support_program_id;
