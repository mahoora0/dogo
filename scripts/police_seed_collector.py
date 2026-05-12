#!/usr/bin/env python3
"""
Collect police lost-item seed data into a dedicated MySQL schema.

The script is intentionally independent from the Spring app because this is a
large data-preparation job, not request-time service logic.
"""

from __future__ import annotations

import argparse
import os
import sys
import time
import urllib.parse
import urllib.request
from urllib.error import HTTPError, URLError
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from datetime import date, datetime, time as dt_time, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple

try:
    import pymysql
except ModuleNotFoundError:
    pymysql = None


LIST_URL = "http://apis.data.go.kr/1320000/LostGoodsInfoInqireService/getLostGoodsInfoAccToClAreaPd"
DETAIL_URL = "http://apis.data.go.kr/1320000/LostGoodsInfoInqireService/getLostGoodsDetailInfo"
NO_IMAGE_MARKERS = ("no_img", "noimage")


@dataclass(frozen=True)
class ListItem:
    atc_id: Optional[str]
    title: Optional[str]
    item_name: Optional[str]
    lost_ymd: Optional[str]
    lost_place: Optional[str]
    category_name: Optional[str]


@dataclass(frozen=True)
class DetailItem:
    atc_id: Optional[str]
    title: Optional[str]
    item_name: Optional[str]
    lost_ymd: Optional[str]
    lost_hour: Optional[str]
    lost_place: Optional[str]
    category_name: Optional[str]
    content: Optional[str]
    lost_area: Optional[str]
    org_name: Optional[str]
    tel: Optional[str]
    image_url: Optional[str]


def main() -> int:
    args = parse_args()
    service_key = args.service_key or os.getenv("POLICE_LOST_ITEM_SERVICE_KEY")
    if not service_key:
        print("POLICE_LOST_ITEM_SERVICE_KEY is required.", file=sys.stderr)
        return 2

    start_date = parse_date(args.start_date) if args.start_date else date.today() - timedelta(days=args.days)
    end_date = parse_date(args.end_date) if args.end_date else date.today()
    if start_date > end_date:
        print("start-date must be before or equal to end-date.", file=sys.stderr)
        return 2

    conn = connect_without_db(args)
    try:
        ensure_database(conn, args.database)
    finally:
        conn.close()

    conn = connect(args)
    try:
        if args.init_schema:
            apply_schema(conn, args.schema)
        if args.reset_police:
            reset_police_data(conn)

        collect(
            conn=conn,
            service_key=service_key,
            start_date=start_date,
            end_date=end_date,
            num_rows=args.num_rows,
            sleep_seconds=args.sleep,
            max_pages=args.max_pages,
            update_existing=args.update_existing,
            duplicate_page_limit=args.duplicate_page_limit,
            retry_count=args.retry_count,
            retry_sleep=args.retry_sleep,
        )
    finally:
        conn.close()

    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Collect police lost-item seed data.")
    parser.add_argument("--host", default=os.getenv("SEED_DB_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("SEED_DB_PORT", "3306")))
    parser.add_argument("--user", default=os.getenv("SEED_DB_USER", "root"))
    parser.add_argument("--password", default=os.getenv("SEED_DB_PASSWORD", os.getenv("DB_PASSWORD", "")))
    parser.add_argument("--database", default=os.getenv("SEED_DB_NAME", "dogo_seed"))
    parser.add_argument("--service-key", default=None)
    parser.add_argument("--days", type=int, default=int(os.getenv("SEED_LOOKBACK_DAYS", "30")))
    parser.add_argument("--start-date", help="YYYYMMDD or YYYY-MM-DD")
    parser.add_argument("--end-date", help="YYYYMMDD or YYYY-MM-DD")
    parser.add_argument("--num-rows", type=int, default=int(os.getenv("SEED_NUM_ROWS", "100")))
    parser.add_argument("--sleep", type=float, default=float(os.getenv("SEED_API_SLEEP_SECONDS", "0.05")))
    parser.add_argument("--max-pages", type=int, default=None)
    parser.add_argument("--duplicate-page-limit", type=int, default=int(os.getenv("SEED_DUPLICATE_PAGE_LIMIT", "0")),
                        help="Stop after N consecutive pages with no new items (ignored with --update-existing).")
    parser.add_argument("--retry-count", type=int, default=int(os.getenv("SEED_API_RETRY_COUNT", "5")))
    parser.add_argument("--retry-sleep", type=float, default=float(os.getenv("SEED_API_RETRY_SLEEP_SECONDS", "2")))
    parser.add_argument("--schema", default="src/main/resources/schema.sql")
    parser.add_argument("--init-schema", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument("--reset-police", action="store_true", help="Delete existing POLICE seed rows before collecting.")
    parser.add_argument("--update-existing", action="store_true", help="Refresh rows that already exist by ATC_ID.")
    return parser.parse_args()


def connect_without_db(args: argparse.Namespace):
    if pymysql is None:
        raise RuntimeError("PyMySQL is required. Install it with: pip install -r scripts/requirements-seed.txt")
    return pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        charset="utf8mb4",
        autocommit=True,
    )


def connect(args: argparse.Namespace):
    if pymysql is None:
        raise RuntimeError("PyMySQL is required. Install it with: pip install -r scripts/requirements-seed.txt")
    return pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        charset="utf8mb4",
        autocommit=False,
    )


def ensure_database(conn, database: str) -> None:
    with conn.cursor() as cur:
        cur.execute(
            f"CREATE DATABASE IF NOT EXISTS `{database}` "
            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
        )


def apply_schema(conn, schema_path: str) -> None:
    sql_path = Path(schema_path)
    if not sql_path.exists():
        raise FileNotFoundError(f"schema file not found: {schema_path}")

    statements = split_sql_statements(sql_path.read_text(encoding="utf-8"))
    with conn.cursor() as cur:
        for statement in statements:
            cur.execute(statement)
    conn.commit()


def split_sql_statements(sql: str) -> List[str]:
    statements: List[str] = []
    current: List[str] = []
    in_single_quote = False
    in_double_quote = False

    for char in sql:
        if char == "'" and not in_double_quote:
            in_single_quote = not in_single_quote
        elif char == '"' and not in_single_quote:
            in_double_quote = not in_double_quote

        if char == ";" and not in_single_quote and not in_double_quote:
            statement = "".join(current).strip()
            if statement:
                statements.append(statement)
            current = []
        else:
            current.append(char)

    tail = "".join(current).strip()
    if tail:
        statements.append(tail)
    return statements


def progress_line(fetched: int, total_count: int, start_time: float) -> str:
    if total_count <= 0:
        return f"{fetched}/??"
    pct = min(fetched * 100 // total_count, 100)
    elapsed = time.time() - start_time
    if fetched > 0 and elapsed > 0:
        remaining = int(elapsed / fetched * (total_count - fetched))
        mins, secs = divmod(remaining, 60)
        eta = f"{mins}m{secs:02d}s" if mins else f"{secs}s"
    else:
        eta = "?"
    return f"{fetched}/{total_count} ({pct}%) ETA {eta}"


def reset_police_data(conn) -> None:
    with conn.cursor() as cur:
        cur.execute("DELETE FROM LOST_ITEM WHERE SOURCE_TYPE = 'POLICE'")
    conn.commit()


def collect(
    conn,
    service_key: str,
    start_date: date,
    end_date: date,
    num_rows: int,
    sleep_seconds: float,
    max_pages: Optional[int],
    update_existing: bool,
    duplicate_page_limit: int = 2,
    retry_count: int = 5,
    retry_sleep: float = 2,
) -> None:
    page_no = 1
    fetched = 0
    saved = 0
    skipped = 0
    updated = 0
    consecutive_duplicate_pages = 0
    stop_on_duplicates = not update_existing and duplicate_page_limit > 0
    start_time = time.time()

    print(f"Collecting police lost items: {start_date}..{end_date}, num_rows={num_rows}")

    while True:
        if max_pages is not None and page_no > max_pages:
            break

        try:
            page = fetch_list_page(service_key, start_date, end_date, page_no, num_rows, retry_count, retry_sleep)
        except Exception as exc:
            print(f"List page failed pageNo={page_no}: {exc}", file=sys.stderr)
            break
        items = page["items"]
        total_count = page["total_count"]
        progress = progress_line(fetched, total_count, start_time)
        print(f"[page {page_no}] {progress} | saved={saved} skipped={skipped} updated={updated}")

        if not items:
            break

        new_candidate_count = 0
        for list_item in items:
            fetched += 1
            try:
                atc_id = clean(list_item.atc_id)
                if not atc_id:
                    skipped += 1
                    continue

                existing_id = find_lost_id(conn, atc_id)
                if existing_id and not update_existing:
                    skipped += 1
                    continue
                if not existing_id:
                    new_candidate_count += 1

                detail = fetch_detail_or_none(service_key, atc_id, retry_count, retry_sleep)
                if sleep_seconds > 0:
                    time.sleep(sleep_seconds)

                if existing_id:
                    update_lost_item(conn, existing_id, list_item, detail)
                    save_image_if_present(conn, existing_id, atc_id, detail)
                    updated += 1
                else:
                    lost_id = insert_lost_item(conn, list_item, detail)
                    save_image_if_present(conn, lost_id, atc_id, detail)
                    saved += 1
                conn.commit()
            except Exception as exc:
                conn.rollback()
                skipped += 1
                print(f"  skip atcId={list_item.atc_id}: {exc}", file=sys.stderr)

        if stop_on_duplicates:
            if new_candidate_count == 0:
                consecutive_duplicate_pages += 1
            else:
                consecutive_duplicate_pages = 0
            if consecutive_duplicate_pages >= duplicate_page_limit:
                print(f"Stopping early: {duplicate_page_limit} consecutive pages with no new items.")
                break

        if total_count and page_no * num_rows >= total_count:
            break
        page_no += 1
        if sleep_seconds > 0:
            time.sleep(sleep_seconds)

    elapsed = int(time.time() - start_time)
    print(f"Done. fetched={fetched} saved={saved} skipped={skipped} updated={updated} elapsed={elapsed}s")


def fetch_list_page(
    service_key: str,
    start_date: date,
    end_date: date,
    page_no: int,
    num_rows: int,
    retry_count: int,
    retry_sleep: float,
) -> dict:
    params = {
        "serviceKey": service_key,
        "START_YMD": start_date.strftime("%Y%m%d"),
        "END_YMD": end_date.strftime("%Y%m%d"),
        "pageNo": str(page_no),
        "numOfRows": str(num_rows),
    }
    root = request_xml(LIST_URL, params, retry_count, retry_sleep)
    ensure_success(root)
    return {
        "total_count": parse_int(text(root, "totalCount")),
        "items": [
            ListItem(
                atc_id=text(item, "atcId"),
                title=text(item, "lstSbjt"),
                item_name=text(item, "lstPrdtNm"),
                lost_ymd=text(item, "lstYmd"),
                lost_place=text(item, "lstPlace"),
                category_name=text(item, "prdtClNm"),
            )
            for item in root.findall(".//item")
        ],
    }


def fetch_detail(service_key: str, atc_id: str, retry_count: int, retry_sleep: float) -> Optional[DetailItem]:
    root = request_xml(DETAIL_URL, {"serviceKey": service_key, "ATC_ID": atc_id}, retry_count, retry_sleep)
    ensure_success(root)
    item = root.find(".//item")
    if item is None:
        return None
    return DetailItem(
        atc_id=text(item, "atcId"),
        title=text(item, "lstSbjt"),
        item_name=text(item, "lstPrdtNm"),
        lost_ymd=text(item, "lstYmd"),
        lost_hour=text(item, "lstHor"),
        lost_place=text(item, "lstPlace"),
        category_name=text(item, "prdtClNm"),
        content=text(item, "uniq"),
        lost_area=text(item, "lstLctNm"),
        org_name=text(item, "orgNm"),
        tel=text(item, "tel"),
        image_url=text(item, "lstFilePathImg"),
    )


def fetch_detail_or_none(service_key: str, atc_id: str, retry_count: int, retry_sleep: float) -> Optional[DetailItem]:
    try:
        return fetch_detail(service_key, atc_id, retry_count, retry_sleep)
    except Exception as exc:
        print(f"  detail failed atcId={atc_id}: {exc}", file=sys.stderr)
        return None


def request_xml(url: str, params: Dict[str, str], retry_count: int, retry_sleep: float) -> ET.Element:
    query = urllib.parse.urlencode(params, safe="%")
    request = urllib.request.Request(f"{url}?{query}", headers={"Accept": "application/xml"})
    last_error: Optional[Exception] = None
    attempts = max(retry_count, 0) + 1
    for attempt in range(1, attempts + 1):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                body = response.read().decode("utf-8", errors="replace")
            return ET.fromstring(body)
        except (HTTPError, URLError, TimeoutError, ET.ParseError) as exc:
            last_error = exc
            if attempt >= attempts:
                break
            wait_seconds = retry_sleep * attempt
            print(f"  request failed ({attempt}/{attempts}) {exc}; retrying in {wait_seconds:.1f}s", file=sys.stderr)
            time.sleep(wait_seconds)
    raise last_error if last_error else RuntimeError("request failed")


def ensure_success(root: ET.Element) -> None:
    result_code = text(root, "resultCode")
    if result_code and result_code != "00":
        result_msg = text(root, "resultMsg") or text(root, "resultMag")
        raise RuntimeError(f"Police API failed: {result_code} {result_msg}")


def find_lost_id(conn, atc_id: str) -> Optional[int]:
    with conn.cursor() as cur:
        cur.execute("SELECT LOST_ID FROM LOST_ITEM WHERE ATC_ID = %s", (atc_id,))
        row = cur.fetchone()
    return row[0] if row else None


def insert_lost_item(conn, list_item: ListItem, detail: Optional[DetailItem]) -> int:
    mapped = map_lost_item(list_item, detail)
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO LOST_ITEM (
              USER_NO, SOURCE_TYPE, ATC_ID, TITLE, CONTENT, ITEM_NAME,
              CATEGORY_ID, CATEGORY_MAIN, CATEGORY_SUB, LOST_AT, LOST_AREA,
              LOST_PLACE, CONTACT, STATUS, IS_DELETED
            )
            VALUES (
              NULL, 'POLICE', %(atc_id)s, %(title)s, %(content)s, %(item_name)s,
              NULL, %(category_main)s, %(category_sub)s, %(lost_at)s, %(lost_area)s,
              %(lost_place)s, %(contact)s, 'WAITING', FALSE
            )
            """,
            mapped,
        )
        return cur.lastrowid


def update_lost_item(conn, lost_id: int, list_item: ListItem, detail: Optional[DetailItem]) -> None:
    mapped = map_lost_item(list_item, detail)
    mapped["lost_id"] = lost_id
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE LOST_ITEM
            SET TITLE = %(title)s,
                CONTENT = %(content)s,
                ITEM_NAME = %(item_name)s,
                CATEGORY_MAIN = %(category_main)s,
                CATEGORY_SUB = %(category_sub)s,
                LOST_AT = %(lost_at)s,
                LOST_AREA = %(lost_area)s,
                LOST_PLACE = %(lost_place)s,
                CONTACT = %(contact)s,
                STATUS = 'WAITING',
                IS_DELETED = FALSE
            WHERE LOST_ID = %(lost_id)s
            """,
            mapped,
        )


def map_lost_item(list_item: ListItem, detail: Optional[DetailItem]) -> dict:
    atc_id = clean(value_or(detail.atc_id if detail else None, list_item.atc_id))
    item_name = clean(value_or(detail.item_name if detail else None, list_item.item_name, detail.title if detail else None, list_item.title))
    title = clean(value_or(detail.title if detail else None, list_item.title, item_name))
    category_main, category_sub = parse_category(value_or(detail.category_name if detail else None, list_item.category_name))

    return {
        "atc_id": atc_id,
        "title": title,
        "content": clean(detail.content if detail else None),
        "item_name": item_name,
        "category_main": category_main,
        "category_sub": category_sub,
        "lost_at": parse_lost_at(
            value_or(detail.lost_ymd if detail else None, list_item.lost_ymd),
            detail.lost_hour if detail else None,
        ),
        "lost_area": clean(detail.lost_area if detail else None),
        "lost_place": clean(value_or(detail.lost_place if detail else None, list_item.lost_place, "장소 미상")),
        "contact": contact(detail.org_name if detail else None, detail.tel if detail else None),
    }


def save_image_if_present(conn, lost_id: int, atc_id: str, detail: Optional[DetailItem]) -> None:
    if detail is None or not is_actual_image_url(detail.image_url):
        return

    image_url = clean(detail.image_url)
    with conn.cursor() as cur:
        cur.execute("SELECT IMAGE_ID FROM LOST_ITEM_IMAGE WHERE LOST_ID = %s LIMIT 1", (lost_id,))
        if cur.fetchone():
            return
        cur.execute(
            """
            INSERT INTO LOST_ITEM_IMAGE (
              LOST_ID, ORIGINAL_NAME, STORED_NAME, IMAGE_URL, CONTENT_TYPE, FILE_SIZE, SORT_ORDER
            )
            VALUES (%s, %s, %s, %s, 'image/external', NULL, 0)
            """,
            (lost_id, original_name(image_url), atc_id, image_url),
        )


def parse_category(category_name: Optional[str]) -> Tuple[Optional[str], Optional[str]]:
    if not category_name:
        return None, None
    parts = [part.strip() for part in category_name.split(">")]
    main = clean(parts[0]) if parts else None
    sub = clean(parts[1]) if len(parts) > 1 else None
    return main, sub


def parse_lost_at(ymd: Optional[str], hour: Optional[str]) -> datetime:
    if not ymd:
        raise ValueError("lost date is required")
    ymd = ymd.strip()
    if len(ymd) == 8 and ymd.isdigit():
        lost_date = datetime.strptime(ymd, "%Y%m%d").date()
    else:
        lost_date = datetime.strptime(ymd, "%Y-%m-%d").date()
    return datetime.combine(lost_date, parse_hour(hour))


def parse_hour(hour: Optional[str]) -> dt_time:
    if not hour:
        return dt_time(0, 0)
    value = hour.strip()
    if value.isdigit():
        hour_int = int(value)
        if 0 <= hour_int <= 23:
            return dt_time(hour_int, 0)
    try:
        return datetime.strptime(value, "%H:%M").time()
    except ValueError:
        return dt_time(0, 0)


def contact(org_name: Optional[str], tel: Optional[str]) -> Optional[str]:
    org_name = clean(org_name)
    tel = clean(tel)
    if org_name and tel:
        return f"{org_name} / {tel}"
    return tel or org_name


def is_actual_image_url(image_url: Optional[str]) -> bool:
    if not image_url:
        return False
    normalized = image_url.strip().lower()
    return normalized.startswith("http") and not any(marker in normalized for marker in NO_IMAGE_MARKERS)


def original_name(image_url: str) -> str:
    path = urllib.parse.urlparse(image_url).path
    if "/" in path:
        filename = path.rsplit("/", 1)[-1]
        if filename:
            return filename
    return "police-image"


def parse_date(value: str) -> date:
    value = value.strip()
    if len(value) == 8 and value.isdigit():
        return datetime.strptime(value, "%Y%m%d").date()
    return datetime.strptime(value, "%Y-%m-%d").date()


def parse_int(value: Optional[str]) -> int:
    return int(value) if value and value.isdigit() else 0


def text(root: ET.Element, tag: str) -> Optional[str]:
    node = root.find(f".//{tag}")
    if node is None or node.text is None:
        return None
    return clean(node.text)


def clean(value: Optional[str]) -> Optional[str]:
    if value is None:
        return None
    stripped = value.strip()
    return stripped or None


def value_or(*values: Optional[str]) -> Optional[str]:
    for value in values:
        cleaned = clean(value)
        if cleaned:
            return cleaned
    return None


if __name__ == "__main__":
    raise SystemExit(main())
