#!/usr/bin/env python3
"""
Mine candidate rows for V17__create_and_seed_btp_connection_keyword_rules.sql.

The script reads the local Postgres data, finds keywords that are grounded by
actual company/support/NTIS source text and matching BTP equipment, then writes:

- btp_connection_keyword_candidates.csv
- btp_connection_keyword_candidates.sql
- btp_connection_existing_rule_review.csv

It intentionally does not mutate the database.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from collections import Counter, defaultdict
from dataclasses import dataclass
from decimal import Decimal
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT_DIR = ROOT / "temp" / "btp-keyword-rule-mining"

SOURCE_SQL = """
with company_divisions as (
    select
        company.company_id,
        coalesce(company_ksic.division_code, history_ksic.division_code) as division_code
    from public.company company
    left join public.ksic_info company_ksic
      on company_ksic.ksic_code = company.ksic_code
    left join public.btp_support_history history
      on history.company_id = company.company_id
    left join public.ksic_info history_ksic
      on history_ksic.ksic_code = history.industry_code
),
source_rows as (
    select distinct
        company.company_id,
        coalesce(nullif(trim(company.company_name), ''), '기업 #' || company.company_id) as company_name,
        division.division_code,
        'company.main_product' as source_field,
        company.main_product as source_text
    from public.company company
    join company_divisions division on division.company_id = company.company_id
    union all
    select distinct
        history.company_id,
        coalesce(nullif(trim(company.company_name), ''), '기업 #' || history.company_id) as company_name,
        coalesce(history_ksic.division_code, company_ksic.division_code) as division_code,
        'btp_support_history.main_product' as source_field,
        history.main_product as source_text
    from public.btp_support_history history
    join public.company company on company.company_id = history.company_id
    left join public.ksic_info history_ksic on history_ksic.ksic_code = history.industry_code
    left join public.ksic_info company_ksic on company_ksic.ksic_code = company.ksic_code
    union all
    select distinct
        history.company_id,
        coalesce(nullif(trim(company.company_name), ''), '기업 #' || history.company_id) as company_name,
        coalesce(history_ksic.division_code, company_ksic.division_code) as division_code,
        'btp_support_history.support_item' as source_field,
        history.support_item as source_text
    from public.btp_support_history history
    join public.company company on company.company_id = history.company_id
    left join public.ksic_info history_ksic on history_ksic.ksic_code = history.industry_code
    left join public.ksic_info company_ksic on company_ksic.ksic_code = company.ksic_code
    union all
    select distinct
        project.company_id,
        coalesce(nullif(trim(company.company_name), ''), '기업 #' || project.company_id) as company_name,
        company_ksic.division_code,
        'company_ntis_lead_project.project_name' as source_field,
        project.project_name as source_text
    from public.company_ntis_lead_project project
    join public.company company on company.company_id = project.company_id
    left join public.ksic_info company_ksic on company_ksic.ksic_code = company.ksic_code
)
select company_id, company_name, division_code, source_field, source_text
from source_rows
where source_text is not null
  and trim(source_text) <> ''
  and (%(division_filter)s is null or division_code = %(division_filter)s)
order by company_id, source_field, source_text
"""

EQUIPMENT_SQL = """
select
    equipment.id as equipment_id,
    equipment.equipment_name,
    equipment.category_large,
    equipment.category_middle,
    equipment.category_small,
    equipment.description,
    equipment.location_name,
    match.hub_id,
    hub.hub_name
from public.btp_equipment equipment
left join public.v_btp_equipment_hub_match match
  on match.equipment_id = equipment.id
left join public.btp_infra_hub hub
  on hub.hub_id = match.hub_id
order by equipment.id
"""

EXISTING_RULE_SQL = """
select
    keyword,
    function_name,
    equipment_category_large,
    equipment_category_middle,
    confidence,
    reviewed,
    active
from public.btp_connection_keyword_rule
order by keyword, function_name, equipment_category_large, equipment_category_middle
"""


STOPWORDS = {
    "개발",
    "기술",
    "사업",
    "지원",
    "제조",
    "제품",
    "부품",
    "시스템",
    "장비",
    "기반",
    "구축",
    "활용",
    "관련",
    "개선",
    "고도화",
    "플랫폼",
    "솔루션",
    "위한",
    "통한",
    "적용",
    "제작",
    "시작품",
    "인증",
    "평가",
    "관리",
    "고정밀",
    "고강성",
    "및",
    "the",
    "and",
    "for",
    "with",
}


FUNCTION_PROFILES = [
    {
        "function_name": "전자파시험(EMC)",
        "keywords": ["전자파", "EMC", "내성", "방사", "전자기", "노이즈"],
        "equipment_any": ["전자파", "EMC", "내성", "전기자기", "안테나", "스펙트럼"],
        "category_large": "전기/전자장비",
        "category_middle": "측정시험장비",
    },
    {
        "function_name": "환경시험",
        "keywords": ["항온항습", "온습도", "열충격", "내열", "고온", "저온", "냉각", "방수", "방진", "습도"],
        "equipment_any": ["항온", "항습", "온도", "습도", "열충격", "냉각", "챔버"],
        "category_large": "기계가공/시험장비",
        "category_middle": "열유체장비",
    },
    {
        "function_name": "진동/충격시험",
        "keywords": ["진동", "충격", "내진", "가속도"],
        "equipment_any": ["진동", "충격", "가속도"],
        "category_large": "물리적측정장비",
        "category_middle": "음향/소음/진동/충격/초음파측정장비",
    },
    {
        "function_name": "압력/유체특성시험",
        "keywords": ["압력", "밸브", "펌프", "유량", "유체", "진공", "토크"],
        "equipment_any": ["압력", "유량", "유체", "진공", "토크", "다이나모"],
        "category_large": "물리적측정장비",
        "category_middle": "힘/토오크/압력/진공측정장비",
    },
    {
        "function_name": "재료물성시험",
        "keywords": ["인장", "강도", "피로", "경도", "파괴", "내구", "소재", "복합재", "금속"],
        "equipment_any": ["인장", "강도", "피로", "경도", "재료", "물성", "만능"],
        "category_large": "기계가공/시험장비",
        "category_middle": "재료물성시험장비",
    },
    {
        "function_name": "성분/분석시험",
        "keywords": ["분석", "성분", "분광", "크로마토", "질량", "가스", "이온", "입자", "미세먼지"],
        "equipment_any": ["분석", "분광", "크로마토", "질량", "입자", "원심", "분리"],
        "category_large": "화합물전처리/분석장비",
        "category_middle": None,
    },
    {
        "function_name": "정밀 관찰/영상분석",
        "keywords": ["현미경", "영상", "카메라", "CCTV", "3차원", "계측", "측정", "스캐너"],
        "equipment_any": ["현미경", "영상", "카메라", "3차원", "측정", "스캐너", "CT"],
        "category_large": "광학/전자영상장비",
        "category_middle": None,
    },
    {
        "function_name": "데이터/실감형 콘텐츠 검증",
        "keywords": ["VR", "AR", "시뮬레이터", "빅데이터", "관제", "소프트웨어", "AI", "IoT"],
        "equipment_any": ["VR", "AR", "시뮬레이터", "데이터", "서버", "소프트웨어", "관제"],
        "category_large": "데이터처리장비",
        "category_middle": None,
    },
    {
        "function_name": "의료기기 사용성/성능평가",
        "keywords": ["의료기기", "헬스케어", "치과", "골이식재", "인솔", "깔창", "신발", "보행"],
        "equipment_any": ["의료", "진단", "보행", "족압", "인체", "임상"],
        "category_large": "임상의료장비",
        "category_middle": None,
    },
]


@dataclass(frozen=True)
class SourceRow:
    company_id: int
    company_name: str
    division_code: str | None
    source_field: str
    source_text: str


@dataclass(frozen=True)
class EquipmentRow:
    equipment_id: int
    equipment_name: str
    category_large: str | None
    category_middle: str | None
    category_small: str | None
    description: str | None
    location_name: str | None
    hub_id: int | None
    hub_name: str | None


def read_dotenv(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for raw_line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def sql_literal(value: str | None) -> str:
    if value is None:
        return "null"
    return "'" + value.replace("'", "''") + "'"


def normalize_text(value: str | None) -> str:
    if not value:
        return ""
    return re.sub(r"\s+", " ", value).strip()


def term_key(value: str) -> str:
    return re.sub(r"\s+", "", value).lower()


def terms(value: str | None) -> set[str]:
    text = normalize_text(value)
    found = set()
    for token in re.findall(r"[가-힣A-Za-z0-9+#/.-]{2,}", text):
        cleaned = token.strip(".,;:()[]{}<>\"'")
        if len(cleaned) < 2:
            continue
        if term_key(cleaned) in STOPWORDS:
            continue
        found.add(cleaned)
    return found


def contains_term(text: str | None, term: str) -> bool:
    return term_key(term) in term_key(text or "")


def truncate(value: str | None, limit: int = 120) -> str:
    text = normalize_text(value)
    if len(text) <= limit:
        return text
    return text[: limit - 1] + "…"


class Db:
    def __init__(self, env: dict[str, str]) -> None:
        self.env = env
        self._connection: Any | None = None
        self._driver = self._connect_python_driver()

    def _connect_python_driver(self) -> str | None:
        try:
            import psycopg  # type: ignore

            self._connection = psycopg.connect(
                host="127.0.0.1",
                port=int(self.env.get("POSTGRES_PORT", "5432")),
                dbname=self.env.get("POSTGRES_DB", "hyeyum"),
                user=self.env.get("POSTGRES_USER", "hyeyum"),
                password=self.env.get("POSTGRES_PASSWORD", "hyeyum"),
            )
            return "psycopg"
        except Exception:
            pass
        try:
            import psycopg2  # type: ignore

            self._connection = psycopg2.connect(
                host="127.0.0.1",
                port=int(self.env.get("POSTGRES_PORT", "5432")),
                dbname=self.env.get("POSTGRES_DB", "hyeyum"),
                user=self.env.get("POSTGRES_USER", "hyeyum"),
                password=self.env.get("POSTGRES_PASSWORD", "hyeyum"),
            )
            return "psycopg2"
        except Exception:
            return None

    def query(self, sql: str, params: dict[str, str | None] | None = None) -> list[dict[str, Any]]:
        rendered_sql = sql
        if params:
            for key, value in params.items():
                rendered_sql = rendered_sql.replace(f"%({key})s", sql_literal(value))

        if self._driver:
            with self._connection.cursor() as cursor:
                cursor.execute(rendered_sql)
                columns = [desc[0] for desc in cursor.description]
                return [dict(zip(columns, row)) for row in cursor.fetchall()]

        json_sql = f"select coalesce(jsonb_agg(row_to_json(q)), '[]'::jsonb)::text from ({rendered_sql}) q"
        command = [
            "docker",
            "compose",
            "exec",
            "-T",
            "db",
            "psql",
            "-U",
            self.env.get("POSTGRES_USER", "hyeyum"),
            "-d",
            self.env.get("POSTGRES_DB", "hyeyum"),
            "-X",
            "-A",
            "-t",
            "-c",
            json_sql,
        ]
        result = subprocess.run(
            command,
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            capture_output=True,
            check=False,
        )
        if result.returncode != 0:
            raise RuntimeError(
                "DB query failed. Install psycopg/psycopg2 or ensure docker compose db is running.\n"
                + result.stderr.strip()
            )
        payload = result.stdout.strip() or "[]"
        return json.loads(payload)


def load_data(db: Db, division_code: str | None) -> tuple[list[SourceRow], list[EquipmentRow], list[dict[str, Any]]]:
    sources = [
        SourceRow(
            company_id=int(row["company_id"]),
            company_name=row["company_name"],
            division_code=row.get("division_code"),
            source_field=row["source_field"],
            source_text=row["source_text"],
        )
        for row in db.query(SOURCE_SQL, {"division_filter": division_code})
    ]
    equipment = [
        EquipmentRow(
            equipment_id=int(row["equipment_id"]),
            equipment_name=row["equipment_name"],
            category_large=row.get("category_large"),
            category_middle=row.get("category_middle"),
            category_small=row.get("category_small"),
            description=row.get("description"),
            location_name=row.get("location_name"),
            hub_id=int(row["hub_id"]) if row.get("hub_id") is not None else None,
            hub_name=row.get("hub_name"),
        )
        for row in db.query(EQUIPMENT_SQL)
    ]
    existing = db.query(EXISTING_RULE_SQL)
    return sources, equipment, existing


def equipment_matches_profile(equipment: EquipmentRow, profile: dict[str, Any]) -> bool:
    if profile["category_large"] and equipment.category_large != profile["category_large"]:
        return False
    if profile["category_middle"] and equipment.category_middle != profile["category_middle"]:
        return False
    equipment_text = " ".join(
        filter(
            None,
            [
                equipment.equipment_name,
                equipment.category_large,
                equipment.category_middle,
                equipment.category_small,
                equipment.description,
            ],
        )
    )
    return any(contains_term(equipment_text, term) for term in profile["equipment_any"])


def confidence(company_count: int, source_count: int, equipment_count: int, direct_overlap: bool) -> Decimal:
    score = Decimal("0.55")
    score += min(Decimal(company_count) * Decimal("0.035"), Decimal("0.18"))
    score += min(Decimal(source_count) * Decimal("0.010"), Decimal("0.10"))
    score += min(Decimal(equipment_count) * Decimal("0.005"), Decimal("0.10"))
    if direct_overlap:
        score += Decimal("0.07")
    return min(score, Decimal("0.97")).quantize(Decimal("0.001"))


def source_examples(rows: list[SourceRow], keyword: str, limit: int) -> str:
    examples = []
    seen = set()
    for index, row in enumerate(rows):
        key = (row.company_id, row.source_field, row.source_text)
        if key in seen or not contains_term(row.source_text, keyword):
            continue
        seen.add(key)
        examples.append(
            f"{row.company_name}#{row.company_id} | {row.source_field} | {truncate(row.source_text)}"
        )
        if len(examples) >= limit:
            break
    return " || ".join(examples)


def equipment_examples(rows: list[EquipmentRow], limit: int) -> str:
    examples = []
    for row in rows[:limit]:
        category = " > ".join(filter(None, [row.category_large, row.category_middle, row.category_small]))
        hub = f" @ {row.hub_name}" if row.hub_name else ""
        examples.append(f"{row.equipment_name}#{row.equipment_id} | {category}{hub}")
    return " || ".join(examples)


def mine_profile_candidates(
    sources: list[SourceRow], equipment: list[EquipmentRow], example_limit: int
) -> list[dict[str, Any]]:
    candidates = []
    for profile in FUNCTION_PROFILES:
        profile_equipment = [row for row in equipment if equipment_matches_profile(row, profile)]
        if not profile_equipment:
            continue
        for keyword in profile["keywords"]:
            matched_sources = [row for row in sources if contains_term(row.source_text, keyword)]
            if not matched_sources:
                continue
            company_ids = {row.company_id for row in matched_sources}
            hub_ids = {row.hub_id for row in profile_equipment if row.hub_id is not None}
            source_fields = sorted({row.source_field for row in matched_sources})
            candidates.append(
                {
                    "keyword": keyword,
                    "function_name": profile["function_name"],
                    "equipment_category_large": profile["category_large"],
                    "equipment_category_middle": profile["category_middle"],
                    "confidence": confidence(
                        len(company_ids),
                        len(matched_sources),
                        len({row.equipment_id for row in profile_equipment}),
                        any(contains_term(row.equipment_name, keyword) for row in profile_equipment),
                    ),
                    "company_count": len(company_ids),
                    "source_match_count": len(matched_sources),
                    "equipment_count": len({row.equipment_id for row in profile_equipment}),
                    "hub_count": len(hub_ids),
                    "source_fields": ", ".join(source_fields),
                    "source_examples": source_examples(matched_sources, keyword, example_limit),
                    "equipment_examples": equipment_examples(profile_equipment, example_limit),
                    "discovery_method": "profile",
                }
            )
    return candidates


def mine_direct_overlap_candidates(
    sources: list[SourceRow], equipment: list[EquipmentRow], example_limit: int
) -> list[dict[str, Any]]:
    source_by_term: dict[str, list[SourceRow]] = defaultdict(list)
    equipment_by_term: dict[str, list[EquipmentRow]] = defaultdict(list)
    display_term: dict[str, str] = {}

    for source in sources:
        for term in terms(source.source_text):
            key = term_key(term)
            display_term.setdefault(key, term)
            source_by_term[key].append(source)

    for row in equipment:
        text = " ".join(
            filter(
                None,
                [row.equipment_name, row.category_large, row.category_middle, row.category_small, row.description],
            )
        )
        for term in terms(text):
            key = term_key(term)
            display_term.setdefault(key, term)
            equipment_by_term[key].append(row)

    candidates = []
    for key in sorted(set(source_by_term) & set(equipment_by_term)):
        if key in STOPWORDS or len(key) < 2:
            continue
        matched_sources = source_by_term[key]
        matched_equipment = equipment_by_term[key]
        if len({row.company_id for row in matched_sources}) < 1 or len({row.equipment_id for row in matched_equipment}) < 1:
            continue
        category_counter = Counter(
            (row.category_large, row.category_middle)
            for row in matched_equipment
            if row.category_large or row.category_middle
        )
        category_large, category_middle = category_counter.most_common(1)[0][0]
        company_ids = {row.company_id for row in matched_sources}
        hub_ids = {row.hub_id for row in matched_equipment if row.hub_id is not None}
        keyword = display_term[key]
        candidates.append(
            {
                "keyword": keyword,
                "function_name": f"{keyword} 관련 장비 검증",
                "equipment_category_large": category_large,
                "equipment_category_middle": category_middle,
                "confidence": confidence(
                    len(company_ids),
                    len(matched_sources),
                    len({row.equipment_id for row in matched_equipment}),
                    True,
                ),
                "company_count": len(company_ids),
                "source_match_count": len(matched_sources),
                "equipment_count": len({row.equipment_id for row in matched_equipment}),
                "hub_count": len(hub_ids),
                "source_fields": ", ".join(sorted({row.source_field for row in matched_sources})),
                "source_examples": source_examples(matched_sources, keyword, example_limit),
                "equipment_examples": equipment_examples(matched_equipment, example_limit),
                "discovery_method": "direct-text-overlap",
            }
        )
    return candidates


def dedupe(candidates: list[dict[str, Any]]) -> list[dict[str, Any]]:
    best: dict[tuple[Any, ...], dict[str, Any]] = {}
    for candidate in candidates:
        key = (
            term_key(candidate["keyword"]),
            candidate["function_name"],
            candidate["equipment_category_large"],
            candidate["equipment_category_middle"],
        )
        previous = best.get(key)
        if previous is None or candidate["confidence"] > previous["confidence"]:
            best[key] = candidate
    return sorted(
        best.values(),
        key=lambda row: (
            -int(row["company_count"]),
            -int(row["source_match_count"]),
            -int(row["equipment_count"]),
            str(row["keyword"]),
        ),
    )


def attach_evidence_templates(candidates: list[dict[str, Any]]) -> list[dict[str, Any]]:
    rows = []
    for index, candidate in enumerate(candidates):
        row = dict(candidate)
        row["evidence_template"] = objective_evidence_template_for(row, index)
        rows.append(row)
    return rows


def review_existing_rules(
    existing: list[dict[str, Any]], sources: list[SourceRow], equipment: list[EquipmentRow], example_limit: int
) -> list[dict[str, Any]]:
    rows = []
    for rule in existing:
        keyword = rule["keyword"]
        matched_sources = [row for row in sources if contains_term(row.source_text, keyword)]
        matched_equipment = [
            row
            for row in equipment
            if (not rule.get("equipment_category_large") or row.category_large == rule.get("equipment_category_large"))
            and (not rule.get("equipment_category_middle") or row.category_middle == rule.get("equipment_category_middle"))
        ]
        rows.append(
            {
                "keyword": keyword,
                "function_name": rule["function_name"],
                "equipment_category_large": rule.get("equipment_category_large"),
                "equipment_category_middle": rule.get("equipment_category_middle"),
                "current_confidence": rule.get("confidence"),
                "reviewed": rule.get("reviewed"),
                "active": rule.get("active"),
                "company_count": len({row.company_id for row in matched_sources}),
                "source_match_count": len(matched_sources),
                "equipment_count": len({row.equipment_id for row in matched_equipment}),
                "hub_count": len({row.hub_id for row in matched_equipment if row.hub_id is not None}),
                "source_examples": source_examples(matched_sources, keyword, example_limit),
                "equipment_examples": equipment_examples(matched_equipment, example_limit),
            }
        )
    return rows


def write_csv(path: Path, rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        path.write_text("", encoding="utf-8-sig")
        return
    with path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def evidence_template_for(row: dict[str, Any], index: int) -> str:
    source_fields = str(row.get("source_fields") or "")
    company_count = int(row.get("company_count") or 0)
    equipment_count = int(row.get("equipment_count") or 0)
    hub_count = int(row.get("hub_count") or 0)

    if "company_ntis_lead_project" in source_fields:
        templates = [
            "NTIS 과제명에서 '{keyword}' 단서가 확인되어 {functionName}과 연결했습니다",
            "{sourceField}의 연구개발 문맥에 '{keyword}'가 포함되어 {functionName} 활용 가능성을 보았습니다",
            "과제 수행 내용의 '{keyword}' 표현이 {functionName} 검토 근거가 됩니다",
        ]
    elif "support_item" in source_fields:
        templates = [
            "지원품목에 '{keyword}'가 드러나 {functionName} 필요성이 있는 항목으로 보았습니다",
            "{sourceField}에 적힌 '{keyword}' 내용을 기준으로 {functionName}과 연결했습니다",
            "지원 이력의 '{keyword}' 표현이 {functionName} 장비 연계 근거로 쓰였습니다",
        ]
    elif "main_product" in source_fields:
        templates = [
            "주요제품의 '{keyword}' 특성을 바탕으로 {functionName} 수요를 추정했습니다",
            "{sourceField}에서 확인한 '{keyword}' 제품군을 {functionName}과 매칭했습니다",
            "기업 제품 설명에 '{keyword}'가 나타나 {functionName} 검토 대상으로 잡았습니다",
        ]
    else:
        templates = [
            "{sourceField}의 '{keyword}' 표현을 근거로 {functionName} 연결 가능성을 확인했습니다",
            "'{keyword}' 단서가 기업 활동 데이터에서 확인되어 {functionName} 후보로 분류했습니다",
            "기업 데이터의 '{keyword}' 문맥을 따라 {functionName}과 이어지는 장비를 찾았습니다",
        ]

    if company_count >= 3 and hub_count >= 3:
        templates.append("여러 기업과 거점에서 '{keyword}' 흐름이 반복되어 {functionName} 근거로 채택했습니다")
    if equipment_count >= 10:
        templates.append("연결 가능한 장비군이 충분해 '{keyword}'를 {functionName} 판단 단서로 삼았습니다")
    if str(row.get("discovery_method")) == "direct-text-overlap":
        templates.append("기업 문구와 장비 정보 양쪽에 '{keyword}'가 함께 나타나 {functionName}으로 연결했습니다")

    return templates[index % len(templates)]


def objective_evidence_template_for(row: dict[str, Any], index: int) -> str:
    source_fields = str(row.get("source_fields") or "")
    company_count = int(row.get("company_count") or 0)
    source_match_count = int(row.get("source_match_count") or 0)
    equipment_count = int(row.get("equipment_count") or 0)
    hub_count = int(row.get("hub_count") or 0)
    category_large = str(row.get("equipment_category_large") or "관련 장비")
    category_middle = str(row.get("equipment_category_middle") or "세부 장비군")
    method = str(row.get("discovery_method") or "")

    if "company_ntis_lead_project" in source_fields:
        templates = [
            f"NTIS 과제와 기업 데이터 {source_match_count}건에서 '{{keyword}}'가 확인되어 {category_large} 기반 {{functionName}}과 연결했습니다",
            f"연구개발 과제 문맥에 '{{keyword}}'가 나타나고 연결 장비 {equipment_count}건이 확인되어 {{functionName}} 근거로 삼았습니다",
            f"{{sourceField}}의 '{{keyword}}' 단서가 {hub_count}개 거점 장비와 이어져 {{functionName}} 검토 대상으로 분류했습니다",
        ]
    elif "support_item" in source_fields:
        templates = [
            f"지원품목 데이터 {source_match_count}건에서 '{{keyword}}'가 반복되고 {category_middle} 분류 장비 {equipment_count}건과 맞물려 {{functionName}}으로 보았습니다",
            f"{{sourceField}}에 적힌 '{{keyword}}'가 {company_count}개 기업 이력에서 확인되어 {{functionName}} 연결 근거가 됩니다",
            f"지원 이력의 '{{keyword}}' 항목이 {hub_count}개 BTP 거점 장비와 연결되어 {{functionName}} 수요로 해석했습니다",
        ]
    elif "main_product" in source_fields:
        templates = [
            f"주요제품 데이터에서 '{{keyword}}'가 확인되고 관련 장비 {equipment_count}건이 있어 {{functionName}}과 매칭했습니다",
            f"기업 제품군의 '{{keyword}}' 특성이 {category_large} 분류와 맞아 {{functionName}} 후보로 잡았습니다",
            f"{{sourceField}}의 '{{keyword}}' 제품 표현이 {company_count}개 기업에서 확인되어 {{functionName}} 근거로 사용했습니다",
        ]
    else:
        templates = [
            f"기업 데이터 {source_match_count}건과 장비 {equipment_count}건에서 '{{keyword}}' 단서가 함께 확인되어 {{functionName}}과 연결했습니다",
            f"'{{keyword}}'가 {company_count}개 기업과 {hub_count}개 거점 장비를 잇는 공통 단서라 {{functionName}} 후보로 분류했습니다",
            f"{{sourceField}}의 '{{keyword}}' 문맥이 {category_middle} 장비군과 겹쳐 {{functionName}} 연결 가능성을 보았습니다",
        ]

    if method == "direct-text-overlap":
        templates.append(
            f"기업 문구와 장비 정보 양쪽에 '{{keyword}}'가 직접 나타나고 장비 {equipment_count}건이 확인되어 {{functionName}}으로 연결했습니다"
        )
    if company_count >= 3 and hub_count >= 3:
        templates.append(
            f"{company_count}개 기업과 {hub_count}개 거점에서 '{{keyword}}' 연결이 반복되어 {{functionName}} 근거로 채택했습니다"
        )
    if equipment_count >= 10:
        templates.append(
            f"{category_large} 분류의 연결 장비가 {equipment_count}건 확인되어 '{{keyword}}'를 {{functionName}} 판단 단서로 삼았습니다"
        )

    return templates[index % len(templates)]


def sanitize_evidence_template(template: Any) -> str:
    text = str(template or "").strip()
    text = text.replace("{source_fields}", "기업/지원 데이터")
    text = text.replace("{sourceFields}", "기업/지원 데이터")
    text = re.sub(r"\{(?!sourceField\}|keyword\}|functionName\})[^}]+\}", "관련 데이터", text)
    return text


def write_sql(path: Path, rows: list[dict[str, Any]]) -> None:
    lines = [
        "-- Candidate seed rows generated by scripts/mine_btp_connection_keyword_rules.py",
        "-- Review manually before replacing V17 seed values.",
        "insert into public.btp_connection_keyword_rule (",
        "    keyword, function_name, equipment_category_large, equipment_category_middle,",
        "    evidence_template, confidence, generated_by, reviewed, active",
        ")",
        "values",
    ]
    values = []
    for index, row in enumerate(rows):
        template = "{sourceField} '{keyword}' 표현에서 {functionName} 수요 확인"
        template = sanitize_evidence_template(row.get("evidence_template") or objective_evidence_template_for(row, index))
        values.append(
            "    ("
            + ", ".join(
                [
                    sql_literal(row["keyword"]),
                    sql_literal(row["function_name"]),
                    sql_literal(row["equipment_category_large"]),
                    sql_literal(row["equipment_category_middle"]),
                    sql_literal(template),
                    str(row["confidence"]),
                    sql_literal("codex-miner"),
                    "true",
                    "true",
                ]
            )
            + ")"
        )
    lines.append(",\n".join(values))
    lines.extend(
        [
            "on conflict (keyword, function_name, equipment_category_large, equipment_category_middle)",
            "do update set",
            "    evidence_template = excluded.evidence_template,",
            "    confidence = excluded.confidence,",
            "    generated_by = excluded.generated_by,",
            "    reviewed = excluded.reviewed,",
            "    active = excluded.active,",
            "    updated_at = now();",
            "",
        ]
    )
    path.write_text("\n".join(lines), encoding="utf-8")


def extract_response_text(payload: dict[str, Any]) -> str:
    if payload.get("output_text"):
        return str(payload["output_text"])
    fragments: list[str] = []
    for item in payload.get("output", []):
        for content in item.get("content", []):
            if content.get("type") in {"output_text", "text"} and content.get("text"):
                fragments.append(str(content["text"]))
    return "\n".join(fragments)


def parse_json_text(text: str) -> Any:
    stripped = text.strip()
    if stripped.startswith("```"):
        stripped = re.sub(r"^```(?:json)?\s*", "", stripped)
        stripped = re.sub(r"\s*```$", "", stripped)
    return json.loads(stripped)


def openai_json_schema() -> dict[str, Any]:
    item_properties = {
        "keyword": {"type": "string"},
        "decision": {"type": "string", "enum": ["keep", "revise", "drop"]},
        "revised_keyword": {"type": ["string", "null"]},
        "revised_function_name": {"type": ["string", "null"]},
        "equipment_category_large": {"type": ["string", "null"]},
        "equipment_category_middle": {"type": ["string", "null"]},
        "confidence": {"type": "number"},
        "evidence_template": {"type": "string"},
        "reason": {"type": "string"},
        "evidence_summary": {"type": "string"},
        "risk_note": {"type": "string"},
        "diversity_tags": {"type": "array", "items": {"type": "string"}},
    }
    return {
        "type": "json_schema",
        "name": "btp_keyword_rule_ai_review",
        "schema": {
            "type": "object",
            "additionalProperties": False,
            "properties": {
                "items": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "additionalProperties": False,
                        "properties": item_properties,
                        "required": list(item_properties),
                    },
                }
            },
            "required": ["items"],
        },
        "strict": True,
    }


def call_openai_json(api_key: str, model: str, candidates: list[dict[str, Any]], timeout_seconds: int) -> list[dict[str, Any]]:
    safe_candidates = [
        {
            "keyword": row["keyword"],
            "function_name": row["function_name"],
            "equipment_category_large": row["equipment_category_large"],
            "equipment_category_middle": row["equipment_category_middle"],
            "confidence": str(row["confidence"]),
            "company_count": row["company_count"],
            "source_match_count": row["source_match_count"],
            "equipment_count": row["equipment_count"],
            "hub_count": row["hub_count"],
            "source_fields": row["source_fields"],
            "source_examples": row["source_examples"],
            "equipment_examples": row["equipment_examples"],
            "discovery_method": row["discovery_method"],
        }
        for row in candidates
    ]
    body = {
        "model": model,
        "instructions": (
            "You are reviewing Korean BTP infrastructure connection keyword rules. "
            "Use only the provided source examples and equipment examples. "
            "Prefer precise industrial test/evaluation functions over broad labels. "
            "Drop generic keywords that can create noisy matches. "
            "Create a natural one-line Korean evidence_template for each kept or revised item. "
            "The evidence_template must keep these placeholders exactly when useful: {sourceField}, {keyword}, {functionName}. "
            "The evidence_template must include at least one objective basis such as company_count, source_match_count, "
            "equipment_count, hub_count, equipment category, source field, or direct text overlap. "
            "Do not repeat the same sentence pattern across the batch. "
            "Return Korean text for evidence_template, reason, evidence_summary, risk_note, and diversity_tags."
        ),
        "input": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "input_text",
                        "text": (
                            "아래 후보 규칙을 검토해줘. 각 항목마다 keep/revise/drop을 판단하고, "
                            "근거가 다양해지도록 source_field, 기업 예시, 장비 예시, 거점/장비 수를 함께 고려해줘. "
                            "너무 넓은 키워드는 drop 또는 더 구체적인 revised_keyword를 제안해줘.\n\n"
                            "evidence_template은 화면에 바로 보일 한 줄 문장이다. "
                            "기계적인 '표현에서 수요 확인' 문장을 피하고, 사람이 쓴 것처럼 다양하게 작성해줘.\n\n"
                            "단, 모든 문장에는 객관적 이유가 있어야 한다. 예: 몇 개 기업/소스/장비/거점에서 확인됐는지, "
                            "어떤 장비 분류와 맞는지, 기업 문구와 장비 정보가 직접 겹치는지 등을 포함해줘.\n\n"
                            + json.dumps(safe_candidates, ensure_ascii=False)
                        ),
                    }
                ],
            }
        ],
        "text": {"format": openai_json_schema()},
    }
    request = urllib.request.Request(
        "https://api.openai.com/v1/responses",
        data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        message = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"OpenAI request failed with HTTP {error.code}: {message}") from error
    parsed = parse_json_text(extract_response_text(payload))
    return parsed.get("items", [])


def enrich_candidates_with_openai(
    candidates: list[dict[str, Any]],
    env: dict[str, str],
    limit: int,
    batch_size: int,
    timeout_seconds: int,
) -> list[dict[str, Any]]:
    api_key = env.get("OPENAI_API_KEY", "").strip()
    if not api_key:
        raise RuntimeError("OPENAI_API_KEY is required for --openai-enrich.")
    model = env.get("OPENAI_MODEL", "gpt-4.1-mini").strip() or "gpt-4.1-mini"
    selected = candidates[:limit]
    reviewed: list[dict[str, Any]] = []
    by_key: dict[str, dict[str, Any]] = {str(row["keyword"]): row for row in selected}

    for start in range(0, len(selected), batch_size):
        batch = selected[start : start + batch_size]
        ai_items = call_openai_json(api_key, model, batch, timeout_seconds)
        for item in ai_items:
            original = by_key.get(str(item.get("keyword")), {})
            reviewed.append(
                {
                    "keyword": item.get("keyword"),
                    "decision": item.get("decision"),
                    "revised_keyword": item.get("revised_keyword"),
                    "original_function_name": original.get("function_name"),
                    "revised_function_name": item.get("revised_function_name"),
                    "equipment_category_large": item.get("equipment_category_large"),
                    "equipment_category_middle": item.get("equipment_category_middle"),
                    "ai_confidence": item.get("confidence"),
                    "miner_confidence": original.get("confidence"),
                    "evidence_template": sanitize_evidence_template(item.get("evidence_template")),
                    "company_count": original.get("company_count"),
                    "source_match_count": original.get("source_match_count"),
                    "equipment_count": original.get("equipment_count"),
                    "hub_count": original.get("hub_count"),
                    "source_fields": original.get("source_fields"),
                    "reason": item.get("reason"),
                    "evidence_summary": item.get("evidence_summary"),
                    "risk_note": item.get("risk_note"),
                    "diversity_tags": ", ".join(item.get("diversity_tags") or []),
                    "source_examples": original.get("source_examples"),
                    "equipment_examples": original.get("equipment_examples"),
                }
            )
        if start + batch_size < len(selected):
            time.sleep(0.5)
    return reviewed


def ai_review_to_seed_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    seed_rows = []
    for row in rows:
        if row.get("decision") == "drop":
            continue
        keyword = row.get("revised_keyword") or row.get("keyword")
        function_name = row.get("revised_function_name") or row.get("original_function_name")
        if not keyword or not function_name:
            continue
        seed_rows.append(
            {
                "keyword": keyword,
                "function_name": function_name,
                "equipment_category_large": row.get("equipment_category_large"),
                "equipment_category_middle": row.get("equipment_category_middle"),
                "confidence": Decimal(str(row.get("ai_confidence") or row.get("miner_confidence") or "0.700")).quantize(
                    Decimal("0.001")
                ),
                "evidence_template": sanitize_evidence_template(row.get("evidence_template"))
                or "기업 데이터의 '{keyword}' 문맥을 따라 {functionName}과 이어지는 장비를 찾았습니다",
            }
        )
    return seed_rows


def main() -> int:
    parser = argparse.ArgumentParser(description="Mine BTP connection keyword rule candidates from local DB data.")
    parser.add_argument("--division-code", help="Limit source companies to a KSIC division code such as 29.")
    parser.add_argument("--top", type=int, default=120, help="Maximum number of candidate rows to write.")
    parser.add_argument("--min-company-count", type=int, default=1, help="Minimum matched company count.")
    parser.add_argument("--example-limit", type=int, default=3, help="Examples per candidate.")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--openai-enrich", action="store_true", help="Ask OpenAI to review candidates with evidence.")
    parser.add_argument("--openai-limit", type=int, default=50, help="Maximum candidates to send to OpenAI.")
    parser.add_argument("--openai-batch-size", type=int, default=10, help="Candidates per OpenAI request.")
    parser.add_argument("--openai-timeout-seconds", type=int, default=180, help="OpenAI request timeout per batch.")
    args = parser.parse_args()

    env = {**read_dotenv(ROOT / ".env"), **os.environ}
    db = Db(env)
    sources, equipment, existing = load_data(db, args.division_code)
    if not sources:
        print("No source rows found. Check --division-code or DB seed data.", file=sys.stderr)
        return 1
    if not equipment:
        print("No equipment rows found. Check btp_equipment seed data.", file=sys.stderr)
        return 1

    candidates = dedupe(
        mine_profile_candidates(sources, equipment, args.example_limit)
        + mine_direct_overlap_candidates(sources, equipment, args.example_limit)
    )
    candidates = [row for row in candidates if int(row["company_count"]) >= args.min_company_count]
    candidates = attach_evidence_templates(candidates[: args.top])

    output_dir = args.output_dir
    output_dir.mkdir(parents=True, exist_ok=True)
    write_csv(output_dir / "btp_connection_keyword_candidates.csv", candidates)
    write_sql(output_dir / "btp_connection_keyword_candidates.sql", candidates)
    write_csv(
        output_dir / "btp_connection_existing_rule_review.csv",
        review_existing_rules(existing, sources, equipment, args.example_limit),
    )
    if args.openai_enrich:
        ai_review = enrich_candidates_with_openai(
            candidates,
            env,
            limit=max(1, args.openai_limit),
            batch_size=max(1, min(args.openai_batch_size, 25)),
            timeout_seconds=max(30, args.openai_timeout_seconds),
        )
        write_csv(output_dir / "btp_connection_keyword_ai_review.csv", ai_review)
        write_sql(output_dir / "btp_connection_keyword_ai_candidates.sql", ai_review_to_seed_rows(ai_review))

    print(f"sources: {len(sources)}")
    print(f"equipment: {len(equipment)}")
    print(f"existing_rules: {len(existing)}")
    print(f"candidate_rules: {len(candidates)}")
    if args.openai_enrich:
        print(f"ai_review_rows: {len(ai_review)}")
    print(f"output_dir: {output_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
