#!/usr/bin/env python
"""Import a large mysqldump seed file (containing binary BLOBs/embeddings) into the local MySQL DB."""

import argparse
import gzip
import os
import subprocess
import sys
from pathlib import Path

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

def find_mysql_cli() -> str:
    # Try finding in PATH
    try:
        subprocess.run(["mysql", "--version"], capture_output=True, check=True)
        return "mysql"
    except (subprocess.CalledProcessError, FileNotFoundError):
        pass

    # Try standard Windows paths
    possible_paths = [
        r"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        r"C:\Program Files\MySQL\MySQL Server 8.1\bin\mysql.exe",
        r"C:\Program Files\MySQL\MySQL Server 8.2\bin\mysql.exe",
        r"C:\Program Files\MySQL\MySQL Server 8.3\bin\mysql.exe",
    ]
    for p in possible_paths:
        if os.path.exists(p):
            return p

    print("Error: Could not find 'mysql' CLI in PATH or standard installation directories.", file=sys.stderr)
    print("Please install MySQL Server or add 'mysql' to your PATH.", file=sys.stderr)
    sys.exit(1)

def main() -> None:
    parser = argparse.ArgumentParser(description="Import a large SQL/SQL.GZ dump with binary blobs into MySQL.")
    parser.add_argument("dump_file", help="Path to .sql or .sql.gz dump file.")
    parser.add_argument("--reset-db", action="store_true", help="Drop and recreate the target database before import.")
    args = parser.parse_args()

    load_dotenv(ROOT_DIR / ".env")

    dump_file = Path(args.dump_file).resolve()
    if not dump_file.exists():
        print(f"Error: Dump file not found at {dump_file}", file=sys.stderr)
        sys.exit(1)

    db_host = os.getenv("DEV_DB_HOST", "127.0.0.1")
    db_port = os.getenv("DEV_DB_PORT", "3306")
    db_user = os.getenv("DEV_DB_USER", os.getenv("DB_USERNAME", "root"))
    db_pass = os.getenv("DEV_DB_PASSWORD", os.getenv("DB_PASSWORD", ""))
    db_name = os.getenv("DEV_DB_NAME", "dogo")

    mysql_cli = find_mysql_cli()
    print(f"Using MySQL CLI: {mysql_cli}")

    # Base connection arguments (excluding database name for drop/recreate)
    conn_args = [
        "-h", db_host,
        "-P", db_port,
        "-u", db_user,
    ]
    if db_pass:
        conn_args.append(f"-p{db_pass}")

    # 1. Reset database if requested
    if args.reset_db:
        print(f"Resetting database '{db_name}'...")
        sql_reset = f"DROP DATABASE IF EXISTS `{db_name}`; CREATE DATABASE `{db_name}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        reset_proc = subprocess.run(
            [mysql_cli] + conn_args + ["-e", sql_reset],
            capture_output=True
        )
        if reset_proc.returncode != 0:
            print("Error resetting database:", file=sys.stderr)
            print(reset_proc.stderr.decode('utf-8', errors='replace'), file=sys.stderr)
            sys.exit(reset_proc.returncode)

    # 2. Decompress dump file
    print(f"Decompressing {dump_file.name}...")
    try:
        if dump_file.suffix == ".gz":
            with gzip.open(dump_file, "rb") as f:
                sql_bytes = f.read()
        else:
            sql_bytes = dump_file.read_bytes()
    except Exception as e:
        print(f"Error reading dump file: {e}", file=sys.stderr)
        sys.exit(1)

    # 3. Stream SQL bytes to mysql.exe
    print(f"Importing {len(sql_bytes)} bytes into database '{db_name}'...")
    import_proc = subprocess.run(
        [mysql_cli] + conn_args + [db_name],
        input=sql_bytes,
        capture_output=True
    )

    if import_proc.returncode == 0:
        print("Database import completed successfully!")
        if import_proc.stderr:
            stderr_str = import_proc.stderr.decode('utf-8', errors='replace').strip()
            # Ignore password warning to keep clean output
            if "Using a password on the command line interface can be insecure" not in stderr_str:
                print(f"Warnings:\n{stderr_str}")
    else:
        print(f"Error: Import failed with exit code {import_proc.returncode}", file=sys.stderr)
        print(import_proc.stderr.decode('utf-8', errors='replace'), file=sys.stderr)
        sys.exit(import_proc.returncode)

if __name__ == "__main__":
    main()
