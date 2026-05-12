#!/usr/bin/env python
"""Import a mysqldump seed file into the local development MySQL database."""

from __future__ import annotations

import argparse
import gzip
import os
from pathlib import Path

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
        key = key.strip()
        value = value.strip().strip("'").strip('"')
        os.environ.setdefault(key, value)


def latest_dump() -> Path:
    dumps = sorted(
        (ROOT_DIR / "seed").glob("*.sql.gz"),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )
    if not dumps:
        raise SystemExit("No dump file found under seed/*.sql.gz. Pass a dump path explicitly.")
    return dumps[0]


def read_dump(path: Path) -> str:
    if path.suffix == ".gz":
        with gzip.open(path, "rt", encoding="utf-8") as dump:
            return dump.read()
    return path.read_text(encoding="utf-8")


def strip_line_comments(statement: str) -> str:
    lines = []
    for line in statement.splitlines():
        if line.lstrip().startswith("--"):
            continue
        lines.append(line)
    return "\n".join(lines).strip()


def iter_sql_statements(sql: str):
    buffer: list[str] = []
    quote: str | None = None
    escaped = False

    for char in sql:
        buffer.append(char)

        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
            continue

        if char in ("'", '"', "`"):
            quote = char
        elif char == ";":
            statement = strip_line_comments("".join(buffer))
            buffer.clear()
            if statement:
                yield statement

    tail = strip_line_comments("".join(buffer))
    if tail:
        yield tail


def connect(database: str | None = None):
    return pymysql.connect(
        host=os.getenv("DEV_DB_HOST", "127.0.0.1"),
        port=int(os.getenv("DEV_DB_PORT", "3306")),
        user=os.getenv("DEV_DB_USER", os.getenv("DB_USERNAME", "root")),
        password=os.getenv("DEV_DB_PASSWORD", os.getenv("DB_PASSWORD", "")),
        database=database,
        charset="utf8mb4",
        autocommit=True,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Import a seed dump into the development DB.")
    parser.add_argument("dump_file", nargs="?", help="Path to .sql or .sql.gz dump. Defaults to newest seed/*.sql.gz.")
    parser.add_argument("--reset-db", action="store_true", help="Drop and recreate the target DB before import.")
    args = parser.parse_args()

    load_dotenv(ROOT_DIR / ".env")

    db_name = os.getenv("DEV_DB_NAME", "dogo")
    dump_file = Path(args.dump_file).resolve() if args.dump_file else latest_dump()
    if not dump_file.exists():
        raise SystemExit(f"Dump file not found: {dump_file}")

    try:
        with connect() as conn:
            with conn.cursor() as cursor:
                if args.reset_db:
                    cursor.execute(f"DROP DATABASE IF EXISTS `{db_name}`")
                cursor.execute(
                    f"CREATE DATABASE IF NOT EXISTS `{db_name}` "
                    "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                )

        sql = read_dump(dump_file)
        count = 0
        with connect(db_name) as conn:
            with conn.cursor() as cursor:
                for statement in iter_sql_statements(sql):
                    cursor.execute(statement)
                    count += 1
    except err.OperationalError as exc:
        raise SystemExit(
            f"MySQL connection failed: {exc}. "
            "Set DEV_DB_PASSWORD or DB_PASSWORD in your environment/.env and retry."
        ) from exc

    print(f"Imported {dump_file} into {db_name} ({count} statements)")


if __name__ == "__main__":
    main()
