#!/usr/bin/env python3
"""
Export the current local PostgreSQL DB as Flyway migrations.

Generated files:
- src/main/resources/db/migration/V1__create_schema.sql
- src/main/resources/db/migration/R__seed_base_data.sql

The seed migration uses INSERT ... ON CONFLICT (primary_key) DO UPDATE, so it
can be safely re-run as a Flyway repeatable migration.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Any


EXCLUDED_TABLES = {
    "flyway_schema_history",
}

PREFERRED_TABLE_ORDER = [
    "company",
    "ksic_info",
    "ksic_bok_industry_mapping",
    "company_industry_benchmark_mapping",
    "industry_benchmark_source",
    "industry_benchmark_metric",
    "industry_benchmark_index",
    "btp_support_program",
    "btp_support_history",
    "btp_equipment",
    "company_business_purpose",
    "company_employment_statistics",
    "company_financial_statistics",
    "company_ntis_lead_project",
    "company_ntis_collaborative_project",
    "company_patent",
    "company_patent_statistics",
]

INDUSTRY_TABLES_WITHOUT_FK = {
    "industry_benchmark_metric",
    "industry_benchmark_index",
    "ksic_bok_industry_mapping",
    "company_industry_benchmark_mapping",
}


@dataclass(frozen=True)
class Column:
    name: str
    data_type: str


@dataclass(frozen=True)
class SequenceInfo:
    table_name: str
    sequence_name: str
    column_name: str


def main() -> int:
    args = parse_args()
    project_root = Path(args.project_root).resolve()
    migration_dir = project_root / "src" / "main" / "resources" / "db" / "migration"
    migration_dir.mkdir(parents=True, exist_ok=True)

    table_names = load_table_names()
    schema_sql = export_schema()
    schema_sql = normalize_schema(schema_sql)
    (migration_dir / "V1__create_schema.sql").write_text(schema_sql, encoding="utf-8")

    metadata = load_metadata(table_names)
    sequence_infos = load_sequence_infos(table_names)
    seed_sql = render_seed_sql(table_names, metadata, sequence_infos, args.batch_size)
    (migration_dir / "R__seed_base_data.sql").write_text(seed_sql, encoding="utf-8")

    print(f"Wrote {migration_dir / 'V1__create_schema.sql'}")
    print(f"Wrote {migration_dir / 'R__seed_base_data.sql'}")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Export current DB to Flyway baseline migrations.")
    parser.add_argument("--project-root", default=".")
    parser.add_argument("--batch-size", type=int, default=200)
    return parser.parse_args()


def docker_exec(command: list[str], input_bytes: bytes | None = None) -> str:
    result = subprocess.run(
        ["docker", "exec", "-i", "hyeyum-postgres", *command],
        input=input_bytes,
        check=True,
        capture_output=True,
    )
    return result.stdout.decode("utf-8")


def psql(sql: str, *extra_args: str) -> str:
    return docker_exec(
        [
            "psql",
            "-U",
            "hyeyum",
            "-d",
            "hyeyum",
            *extra_args,
            "-c",
            sql,
        ]
    )


def export_schema() -> str:
    return docker_exec(
        [
            "pg_dump",
            "-U",
            "hyeyum",
            "-d",
            "hyeyum",
            "--schema-only",
            "--no-owner",
            "--no-privileges",
            "--schema=public",
            "--exclude-table=public.flyway_schema_history",
        ]
    )


def normalize_schema(schema_sql: str) -> str:
    lines = []
    statement: list[str] = []
    for line in schema_sql.splitlines():
        if line.startswith("\\restrict") or line.startswith("\\unrestrict"):
            continue
        if line == "CREATE SCHEMA public;":
            line = "CREATE SCHEMA IF NOT EXISTS public;"
        statement.append(line)
        if line.endswith(";"):
            if not should_skip_schema_statement(statement):
                lines.extend(statement)
            statement = []
    if statement and not should_skip_schema_statement(statement):
        lines.extend(statement)
    return "\n".join(lines).strip() + "\n"


def should_skip_schema_statement(statement: list[str]) -> bool:
    text = "\n".join(statement)
    if " FOREIGN KEY " not in text:
        return False
    return any(f"ALTER TABLE ONLY public.{table}" in text for table in INDUSTRY_TABLES_WITHOUT_FK)


def load_table_names() -> list[str]:
    rows = json_rows(
        """
        select table_name
        from information_schema.tables
        where table_schema = 'public'
          and table_type = 'BASE TABLE'
          and table_name <> all(array['flyway_schema_history'])
        order by table_name
        """
    )
    table_names = [row["table_name"] for row in rows if row["table_name"] not in EXCLUDED_TABLES]
    return sort_tables_by_dependencies(table_names)


def sort_tables_by_dependencies(table_names: list[str]) -> list[str]:
    table_set = set(table_names)
    preferred_index = {table: index for index, table in enumerate(PREFERRED_TABLE_ORDER)}
    dependencies: dict[str, set[str]] = {table: set() for table in table_names}
    rows = json_rows(
        """
        select child.relname as table_name,
               parent.relname as referenced_table_name
        from pg_constraint constraint_info
        join pg_class child on child.oid = constraint_info.conrelid
        join pg_namespace child_namespace on child_namespace.oid = child.relnamespace
        join pg_class parent on parent.oid = constraint_info.confrelid
        join pg_namespace parent_namespace on parent_namespace.oid = parent.relnamespace
        where constraint_info.contype = 'f'
          and child_namespace.nspname = 'public'
          and parent_namespace.nspname = 'public'
        """
    )
    for row in rows:
        table_name = row["table_name"]
        referenced_table_name = row["referenced_table_name"]
        if table_name in table_set and referenced_table_name in table_set and table_name != referenced_table_name:
            dependencies[table_name].add(referenced_table_name)

    sorted_tables: list[str] = []
    remaining = set(table_names)
    while remaining:
        ready = [table for table in remaining if not dependencies[table] & remaining]
        if not ready:
            ready = list(remaining)
        ready.sort(key=lambda table: (preferred_index.get(table, len(PREFERRED_TABLE_ORDER)), table))
        table = ready[0]
        sorted_tables.append(table)
        remaining.remove(table)
    return sorted_tables


def load_metadata(table_names: list[str]) -> dict[str, list[Column]]:
    rows = json_rows(
        """
        select table_name, column_name, data_type
        from information_schema.columns
        where table_schema = 'public'
        order by table_name, ordinal_position
        """
    )
    result: dict[str, list[Column]] = {table: [] for table in table_names}
    for row in rows:
        table_name = row["table_name"]
        if table_name in result:
            result[table_name].append(Column(row["column_name"], row["data_type"]))
    return result


def load_primary_key_columns(table_name: str) -> list[str]:
    rows = json_rows(
        f"""
        select a.attname as column_name
        from pg_index i
        join pg_class c on c.oid = i.indrelid
        join pg_namespace n on n.oid = c.relnamespace
        join pg_attribute a on a.attrelid = c.oid and a.attnum = any(i.indkey)
        where n.nspname = 'public'
          and c.relname = {sql_literal(table_name)}
          and i.indisprimary
        order by array_position(i.indkey, a.attnum)
        """
    )
    return [row["column_name"] for row in rows]


def load_sequence_infos(table_names: list[str]) -> list[SequenceInfo]:
    table_array = ", ".join(sql_literal(table_name) for table_name in table_names)
    rows = json_rows(
        f"""
        select c.relname as table_name,
               pg_get_serial_sequence(format('%I.%I', n.nspname, c.relname), a.attname) as sequence_name,
               a.attname as column_name
        from pg_class c
        join pg_namespace n on n.oid = c.relnamespace
        join pg_attribute a on a.attrelid = c.oid
        where n.nspname = 'public'
          and c.relname = any(array[{table_array}])
          and a.attnum > 0
          and not a.attisdropped
          and pg_get_serial_sequence(format('%I.%I', n.nspname, c.relname), a.attname) is not null
        order by c.relname
        """
    )
    return [
        SequenceInfo(row["table_name"], row["sequence_name"], row["column_name"])
        for row in rows
    ]


def json_rows(sql: str) -> list[dict[str, Any]]:
    output = psql(
        f"select coalesce(json_agg(row_to_json(rows)), '[]'::json) from ({sql}) rows",
        "-t",
        "-A",
    )
    return json.loads(output.strip() or "[]")


def table_rows(table_name: str, order_columns: list[str]) -> list[dict[str, Any]]:
    order_by = ", ".join(quote_ident(column) for column in order_columns)
    return json_rows(f"select * from public.{quote_ident(table_name)} order by {order_by}")


def render_seed_sql(
    table_names: list[str],
    metadata: dict[str, list[Column]],
    sequence_infos: list[SequenceInfo],
    batch_size: int,
) -> str:
    lines = [
        "-- Generated by scripts/export_flyway_baseline.py",
        "-- Repeatable baseline seed from the current local DB.",
        "begin;",
        "set constraints all deferred;",
        "",
    ]

    for table_name in table_names:
        columns = metadata[table_name]
        if not columns:
            continue
        pk_columns = load_primary_key_columns(table_name)
        if not pk_columns:
            raise RuntimeError(f"No primary key found for {table_name}")
        rows = table_rows(table_name, pk_columns)
        if not rows:
            lines.append(f"-- {table_name}: no rows")
            lines.append("")
            continue
        lines.extend(render_table_upserts(table_name, columns, pk_columns, rows, batch_size))
        lines.append("")

    for sequence_info in sequence_infos:
        lines.append(
            "select setval("
            f"{sql_literal(sequence_info.sequence_name)}, "
            f"coalesce((select max({quote_ident(sequence_info.column_name)}) from public.{quote_ident(sequence_info.table_name)}), 0) + 1, "
            "false"
            ");"
        )

    lines.extend(["", "commit;", ""])
    return "\n".join(lines)


def render_table_upserts(
    table_name: str,
    columns: list[Column],
    pk_columns: list[str],
    rows: list[dict[str, Any]],
    batch_size: int,
) -> list[str]:
    result: list[str] = [f"-- {table_name}: {len(rows)} rows"]
    column_names = [column.name for column in columns]
    column_list = ", ".join(quote_ident(column) for column in column_names)
    conflict_target = ", ".join(quote_ident(column) for column in pk_columns)
    update_columns = [column for column in column_names if column not in pk_columns]
    update_clause = ",\n    ".join(
        f"{quote_ident(column)} = excluded.{quote_ident(column)}" for column in update_columns
    )
    if not update_clause:
        update_clause = f"{quote_ident(pk_columns[0])} = excluded.{quote_ident(pk_columns[0])}"

    for start in range(0, len(rows), batch_size):
        chunk = rows[start : start + batch_size]
        values = []
        for row in chunk:
            values.append("(" + ", ".join(sql_literal(row.get(column)) for column in column_names) + ")")
        result.extend(
            [
                f"insert into public.{quote_ident(table_name)} ({column_list}) values",
                ",\n".join(values),
                f"on conflict ({conflict_target}) do update set",
                f"    {update_clause};",
            ]
        )
    return result


def quote_ident(identifier: str) -> str:
    return '"' + identifier.replace('"', '""') + '"'


def sql_literal(value: Any) -> str:
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return str(value)
    text = str(value)
    return "'" + text.replace("'", "''") + "'"


if __name__ == "__main__":
    raise SystemExit(main())
