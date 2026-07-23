create table if not exists public.btp_connection_keyword_rule (
    rule_id bigserial primary key,
    keyword varchar(100) not null,
    function_name varchar(100) not null,
    equipment_category_large varchar(100),
    equipment_category_middle varchar(100),
    evidence_template varchar(500) not null,
    confidence numeric(4, 3) not null default 0.700,
    generated_by varchar(50) not null default 'manual',
    reviewed boolean not null default true,
    active boolean not null default true,
    created_at timestamp without time zone not null default now(),
    updated_at timestamp without time zone not null default now(),
    constraint ck_btp_connection_keyword_rule_confidence check (confidence >= 0 and confidence <= 1),
    constraint uk_btp_connection_keyword_rule unique (
        keyword,
        function_name,
        equipment_category_large,
        equipment_category_middle
    )
);

comment on table public.btp_connection_keyword_rule is 'BTP 솔루션 3번 연결 근거용 기업/제품 키워드-기능-장비분류 매핑 규칙';
comment on column public.btp_connection_keyword_rule.keyword is '기업 주요제품, 지원품목, NTIS 과제명 등에서 찾을 키워드';
comment on column public.btp_connection_keyword_rule.function_name is '화면에 표시할 연결 기능명';
comment on column public.btp_connection_keyword_rule.equipment_category_large is '연결 대상 장비 대분류. null이면 대분류 제한 없음';
comment on column public.btp_connection_keyword_rule.equipment_category_middle is '연결 대상 장비 중분류. null이면 중분류 제한 없음';
comment on column public.btp_connection_keyword_rule.evidence_template is '연결 근거 문장 템플릿. {sourceField}, {keyword}, {functionName} 치환 예정';
comment on column public.btp_connection_keyword_rule.confidence is 'AI 초안 규칙 신뢰도. 서비스 적용 전 검수 참고값';
comment on column public.btp_connection_keyword_rule.generated_by is '규칙 생성 주체. 예: codex-ai-draft, human';
comment on column public.btp_connection_keyword_rule.reviewed is '사람 검수 완료 여부. 운영 API는 true 규칙만 사용하는 것을 권장';

create index if not exists idx_btp_connection_keyword_rule_active
    on public.btp_connection_keyword_rule (active, reviewed);

create index if not exists idx_btp_connection_keyword_rule_keyword
    on public.btp_connection_keyword_rule (keyword);

create index if not exists idx_btp_connection_keyword_rule_equipment_category
    on public.btp_connection_keyword_rule (equipment_category_large, equipment_category_middle);

insert into public.btp_connection_keyword_rule (
    keyword,
    function_name,
    equipment_category_large,
    equipment_category_middle,
    evidence_template,
    confidence,
    generated_by,
    reviewed,
    active
)
values
    ('전자파', '전자파시험(EMC)', '전기/전자장비', '측정시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.950, 'codex-ai-draft', true, true),
    ('EMC', '전자파시험(EMC)', '전기/전자장비', '측정시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.950, 'codex-ai-draft', true, true),
    ('내성', '전자파시험(EMC)', '전기/전자장비', '측정시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.820, 'codex-ai-draft', true, true),
    ('방사', '전자파시험(EMC)', '전기/전자장비', '측정시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),
    ('고전압', '전기특성시험', '전기/전자장비', '측정시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.900, 'codex-ai-draft', true, true),
    ('전장', '전기특성시험', '전기/전자장비', '측정시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.820, 'codex-ai-draft', true, true),
    ('센서', '전기특성시험', '전기/전자장비', '측정시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),
    ('스마트센서', '전기특성시험', '전기/전자장비', '측정시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.850, 'codex-ai-draft', true, true),

    ('반도체', '반도체 공정/소자 시험', '기계가공/시험장비', '반도체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.930, 'codex-ai-draft', true, true),
    ('웨이퍼', '반도체 공정/소자 시험', '기계가공/시험장비', '반도체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.900, 'codex-ai-draft', true, true),
    ('소자', '반도체 공정/소자 시험', '기계가공/시험장비', '반도체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.760, 'codex-ai-draft', true, true),
    ('마이크로', '정밀 관찰/분석', '광학/전자영상장비', '현미경', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.720, 'codex-ai-draft', true, true),

    ('온습도', '환경시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.930, 'codex-ai-draft', true, true),
    ('항온항습', '환경시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.950, 'codex-ai-draft', true, true),
    ('열충격', '환경시험', '기계가공/시험장비', '재료물성시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.920, 'codex-ai-draft', true, true),
    ('내열', '환경시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.840, 'codex-ai-draft', true, true),
    ('냉각', '환경시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),
    ('저온', '환경시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),
    ('고온', '환경시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),
    ('방수', '환경시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),
    ('방진', '환경시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.760, 'codex-ai-draft', true, true),
    ('미세먼지', '환경성능시험', '화합물전처리/분석장비', '입자분석장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.900, 'codex-ai-draft', true, true),
    ('공기청정', '환경성능시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.880, 'codex-ai-draft', true, true),
    ('환기', '환경성능시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),

    ('진동', '진동/충격시험', '물리적측정장비', '음향/소음/진동/충격/초음파측정장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.920, 'codex-ai-draft', true, true),
    ('소음', '진동/소음시험', '물리적측정장비', '음향/소음/진동/충격/초음파측정장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.880, 'codex-ai-draft', true, true),
    ('충격', '진동/충격시험', '물리적측정장비', '음향/소음/진동/충격/초음파측정장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.860, 'codex-ai-draft', true, true),
    ('압력', '압력/유체특성시험', '물리적측정장비', '힘/토오크/압력/진공측정장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.890, 'codex-ai-draft', true, true),
    ('밸브', '압력/유체특성시험', '물리적측정장비', '힘/토오크/압력/진공측정장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.850, 'codex-ai-draft', true, true),
    ('펌프', '압력/유체특성시험', '물리적측정장비', '유체특성측정장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.800, 'codex-ai-draft', true, true),
    ('토크', '기계성능시험', '물리적측정장비', '힘/토오크/압력/진공측정장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.830, 'codex-ai-draft', true, true),
    ('인장', '재료물성시험', '기계가공/시험장비', '재료물성시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.900, 'codex-ai-draft', true, true),
    ('강도', '재료물성시험', '기계가공/시험장비', '재료물성시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.820, 'codex-ai-draft', true, true),
    ('소재', '재료물성시험', '기계가공/시험장비', '재료물성시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.740, 'codex-ai-draft', true, true),
    ('부품', '재료물성시험', '기계가공/시험장비', '재료물성시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.700, 'codex-ai-draft', true, true),
    ('성능평가', '성능평가시험', '기계가공/시험장비', '재료물성시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.820, 'codex-ai-draft', true, true),

    ('분광', '성분/분광분석', '화합물전처리/분석장비', '분광분석장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.930, 'codex-ai-draft', true, true),
    ('분석', '성분/분석시험', '화합물전처리/분석장비', '분리분석장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.760, 'codex-ai-draft', true, true),
    ('원심', '분리/정제', '화합물전처리/분석장비', '분리정제장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.880, 'codex-ai-draft', true, true),
    ('농축', '분리/정제', '화합물전처리/분석장비', '분리정제장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.850, 'codex-ai-draft', true, true),
    ('필터', '분리/정제', '화합물전처리/분석장비', '분리정제장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),
    ('의약품', '성분/분석시험', '화합물전처리/분석장비', '분리분석장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),
    ('화장품', '성분/분석시험', '화합물전처리/분석장비', '분리분석장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.760, 'codex-ai-draft', true, true),

    ('현미경', '정밀 관찰/분석', '광학/전자영상장비', '현미경', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.940, 'codex-ai-draft', true, true),
    ('영상', '영상/광학분석', '광학/전자영상장비', '카메라/영상처리장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),
    ('카메라', '영상/광학분석', '광학/전자영상장비', '카메라/영상처리장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.890, 'codex-ai-draft', true, true),
    ('CCTV', '영상/광학분석', '광학/전자영상장비', '카메라/영상처리장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.820, 'codex-ai-draft', true, true),
    ('3차원', '3차원 계측', '물리적측정장비', '길이/위치/자세측정장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.880, 'codex-ai-draft', true, true),
    ('계측', '정밀 계측', '물리적측정장비', '길이/위치/자세측정장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.820, 'codex-ai-draft', true, true),
    ('측정', '정밀 계측', '물리적측정장비', '길이/위치/자세측정장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.760, 'codex-ai-draft', true, true),

    ('의료기기', '의료기기 사용성/성능평가', '임상의료장비', '임상측정/진단장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.900, 'codex-ai-draft', true, true),
    ('치과', '의료기기 사용성/성능평가', '임상의료장비', '임상측정/진단장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.840, 'codex-ai-draft', true, true),
    ('골이식재', '의료기기 사용성/성능평가', '임상의료장비', '임상측정/진단장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.860, 'codex-ai-draft', true, true),
    ('헬스케어', '의료/헬스케어 평가', '임상의료장비', '임상측정/진단장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.820, 'codex-ai-draft', true, true),
    ('인솔', '인체영향/착용성 평가', '임상의료장비', '임상측정/진단장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.840, 'codex-ai-draft', true, true),
    ('깔창', '인체영향/착용성 평가', '임상의료장비', '임상측정/진단장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.840, 'codex-ai-draft', true, true),
    ('신발', '신발 성능평가', '임상의료장비', '임상측정/진단장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.820, 'codex-ai-draft', true, true),

    ('드론', '드론/자동화 실증', '기계가공/시험장비', '자동화/이송 장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.900, 'codex-ai-draft', true, true),
    ('관제', '데이터/관제 시스템 검증', '데이터처리장비', '하드웨어', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.830, 'codex-ai-draft', true, true),
    ('VR', '실감형 콘텐츠/시뮬레이션', '데이터처리장비', '장비소프트웨어', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.840, 'codex-ai-draft', true, true),
    ('AR', '실감형 콘텐츠/시뮬레이션', '데이터처리장비', '장비소프트웨어', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.820, 'codex-ai-draft', true, true),
    ('시뮬레이터', '실감형 콘텐츠/시뮬레이션', '데이터처리장비', '장비소프트웨어', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.820, 'codex-ai-draft', true, true),
    ('빅데이터', '데이터 처리/분석', '데이터처리장비', '하드웨어', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.840, 'codex-ai-draft', true, true),
    ('소프트웨어', '데이터 처리/분석', '데이터처리장비', '장비소프트웨어', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.780, 'codex-ai-draft', true, true),

    ('선박', '조선해양 부품 시험', '기계가공/시험장비', '재료물성시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.840, 'codex-ai-draft', true, true),
    ('조선', '조선해양 부품 시험', '기계가공/시험장비', '재료물성시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.840, 'codex-ai-draft', true, true),
    ('해양', '조선해양 부품 시험', '기계가공/시험장비', '재료물성시험장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.800, 'codex-ai-draft', true, true),
    ('소화장치', '환경/안전 시험', '기계가공/시험장비', '열유체장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.760, 'codex-ai-draft', true, true),
    ('구난', '해양 안전 실증', '환경조성/생산/사육시설장비', '환경조성형시설장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.740, 'codex-ai-draft', true, true),
    ('안전', '안전성 평가', '환경조성/생산/사육시설장비', '환경조성형시설장비', '{sourceField} ''{keyword}'' 표현에서 {functionName} 수요 확인', 0.720, 'codex-ai-draft', true, true)
on conflict (
    keyword,
    function_name,
    equipment_category_large,
    equipment_category_middle
)
do nothing;

