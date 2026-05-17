#!/usr/bin/env python
"""Build a recent-window police seed dump from the seed MySQL database."""

from __future__ import annotations

import argparse
import gzip
import os
from datetime import date, datetime, timedelta
from decimal import Decimal
from pathlib import Path
from typing import Any

import pymysql
from pymysql import err


ROOT_DIR = Path(__file__).resolve().parents[1]


def load_dotenv(path: Path) -> None:
    if not path.exists():
        return

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip("'").strip('"'))


def connect(database: str | None = None):
    return pymysql.connect(
        host=os.getenv("SEED_DB_HOST", "127.0.0.1"),
        port=int(os.getenv("SEED_DB_PORT", "3306")),
        user=os.getenv("SEED_DB_USER", os.getenv("DB_USERNAME", "root")),
        password=os.getenv("SEED_DB_PASSWORD", os.getenv("DB_PASSWORD", "")),
        database=database,
        charset="utf8mb4",
        autocommit=True,
    )


def table_columns(conn, database: str, table: str) -> list[str]:
    with conn.cursor() as cursor:
        cursor.execute(
            """
            SELECT COLUMN_NAME
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
            ORDER BY ORDINAL_POSITION
            """,
            (database, table),
        )
        return [row[0] for row in cursor.fetchall()]


def sql_literal(conn, value: Any) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, int):
        return str(value)
    if isinstance(value, Decimal):
        return format(value, "f")
    if isinstance(value, float):
        return repr(value)
    if isinstance(value, datetime):
        return "'" + value.strftime("%Y-%m-%d %H:%M:%S") + "'"
    if isinstance(value, date):
        return "'" + value.strftime("%Y-%m-%d") + "'"
    if isinstance(value, (bytes, bytearray)):
        return "0x" + bytes(value).hex()
    return conn.escape(str(value))


def write_table(
    *,
    conn,
    database: str,
    output,
    table: str,
    where: str,
    params: tuple[Any, ...],
    batch_size: int,
) -> int:
    columns = table_columns(conn, database, table)
    if not columns:
        raise RuntimeError(f"Table not found in seed DB: {database}.{table}")

    column_sql = ",".join(f"`{column}`" for column in columns)
    order_column = columns[0]
    total = 0

    with conn.cursor() as cursor:
        cursor.execute(
            f"SELECT {column_sql} FROM `{table}` WHERE {where} ORDER BY `{order_column}`",
            params,
        )
        while True:
            rows = cursor.fetchmany(batch_size)
            if not rows:
                break

            values = [
                "(" + ",".join(sql_literal(conn, value) for value in row) + ")"
                for row in rows
            ]
            output.write(f"INSERT INTO `{table}` ({column_sql}) VALUES\n")
            output.write(",\n".join(values))
            output.write(";\n\n")
            total += len(rows)

    return total


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create seed/dogo_seed_recent15.sql.gz from recent police rows in dogo_seed."
    )
    parser.add_argument("--days", type=int, default=15, help="Inclusive recent window size. Default: 15.")
    parser.add_argument("--database", default=os.getenv("SEED_DB_NAME", "dogo_seed"))
    parser.add_argument("--schema", default="src/main/resources/schema.sql")
    parser.add_argument("--output", default="seed/dogo_seed_recent15.sql.gz")
    parser.add_argument("--batch-size", type=int, default=500)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.days < 1:
        raise SystemExit("--days must be at least 1.")

    load_dotenv(ROOT_DIR / ".env")

    schema_path = (ROOT_DIR / args.schema).resolve()
    output_path = (ROOT_DIR / args.output).resolve()
    if not schema_path.exists():
        raise SystemExit(f"Schema file not found: {schema_path}")

    try:
        with connect(args.database) as conn:
            with conn.cursor() as cursor:
                cursor.execute("SELECT MAX(DATE(LOST_AT)) FROM LOST_ITEM WHERE SOURCE_TYPE = 'POLICE'")
                max_lost = cursor.fetchone()[0]
                cursor.execute("SELECT MAX(DATE(FOUND_AT)) FROM FOUND_ITEM WHERE SOURCE_TYPE = 'POLICE'")
                max_found = cursor.fetchone()[0]

            max_dates = [value for value in (max_lost, max_found) if value is not None]
            if not max_dates:
                raise SystemExit(f"No POLICE rows found in seed DB: {args.database}")

            end_date = max(max_dates)
            start_date = end_date - timedelta(days=args.days - 1)
            schema_sql = schema_path.read_text(encoding="utf-8").rstrip() + "\n"

            output_path.parent.mkdir(parents=True, exist_ok=True)
            with gzip.open(output_path, "wt", encoding="utf-8", compresslevel=9) as output:
                output.write(f"-- Filtered seed generated from {args.database} using current schema.sql\n")
                output.write(
                    f"-- Max police date: {end_date:%Y-%m-%d}, "
                    f"cutoff date: {start_date:%Y-%m-%d}, inclusive {args.days}-day window\n\n"
                )
                output.write(schema_sql)
                output.write("\nSET FOREIGN_KEY_CHECKS=0;\n\n")

                counts = {
                    "LOST_ITEM": write_table(
                        conn=conn,
                        database=args.database,
                        output=output,
                        table="LOST_ITEM",
                        where="SOURCE_TYPE = 'POLICE' AND DATE(LOST_AT) BETWEEN %s AND %s",
                        params=(start_date, end_date),
                        batch_size=args.batch_size,
                    ),
                    "FOUND_ITEM": write_table(
                        conn=conn,
                        database=args.database,
                        output=output,
                        table="FOUND_ITEM",
                        where="SOURCE_TYPE = 'POLICE' AND DATE(FOUND_AT) BETWEEN %s AND %s",
                        params=(start_date, end_date),
                        batch_size=args.batch_size,
                    ),
                    "LOST_ITEM_IMAGE": write_table(
                        conn=conn,
                        database=args.database,
                        output=output,
                        table="LOST_ITEM_IMAGE",
                        where=(
                            "LOST_ID IN ("
                            "SELECT LOST_ID FROM LOST_ITEM "
                            "WHERE SOURCE_TYPE = 'POLICE' AND DATE(LOST_AT) BETWEEN %s AND %s"
                            ")"
                        ),
                        params=(start_date, end_date),
                        batch_size=args.batch_size,
                    ),
                    "FOUND_ITEM_IMAGE": write_table(
                        conn=conn,
                        database=args.database,
                        output=output,
                        table="FOUND_ITEM_IMAGE",
                        where=(
                            "FOUND_ID IN ("
                            "SELECT FOUND_ID FROM FOUND_ITEM "
                            "WHERE SOURCE_TYPE = 'POLICE' AND DATE(FOUND_AT) BETWEEN %s AND %s"
                            ")"
                        ),
                        params=(start_date, end_date),
                        batch_size=args.batch_size,
                    ),
                }

                output.write("SET FOREIGN_KEY_CHECKS=1;\n")
    except err.OperationalError as exc:
        raise SystemExit(
            f"MySQL connection failed: {exc}. "
            "Set SEED_DB_PASSWORD or DB_PASSWORD in your environment/.env and retry."
        ) from exc

    print(f"Wrote {output_path}")
    print(f"window={start_date:%Y-%m-%d}..{end_date:%Y-%m-%d}")
    for table, count in counts.items():
        print(f"{table}={count}")


if __name__ == "__main__":
    main()
