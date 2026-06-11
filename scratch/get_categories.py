import pymysql
import os

host = os.getenv("SEED_DB_HOST", "127.0.0.1")
port = int(os.getenv("SEED_DB_PORT", "3306"))
user = os.getenv("SEED_DB_USER", "root")
password = os.getenv("SEED_DB_PASSWORD", "")
database = os.getenv("SEED_DB_NAME", "dogo")

try:
    conn = pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
        charset="utf8mb4"
    )
    with conn.cursor() as cur:
        cur.execute("SELECT DISTINCT CATEGORY_MAIN FROM LOST_ITEM WHERE CATEGORY_MAIN IS NOT NULL")
        lost_mains = [row[0] for row in cur.fetchall()]
        
        cur.execute("SELECT DISTINCT CATEGORY_MAIN FROM FOUND_ITEM WHERE CATEGORY_MAIN IS NOT NULL")
        found_mains = [row[0] for row in cur.fetchall()]
        
        print("Lost Category Main:")
        print(lost_mains)
        print("\nFound Category Main:")
        print(found_mains)
        
        cur.execute("SELECT DISTINCT CATEGORY_MAIN, CATEGORY_SUB FROM LOST_ITEM WHERE CATEGORY_MAIN IS NOT NULL")
        lost_pairs = cur.fetchall()
        cur.execute("SELECT DISTINCT CATEGORY_MAIN, CATEGORY_SUB FROM FOUND_ITEM WHERE CATEGORY_MAIN IS NOT NULL")
        found_pairs = cur.fetchall()
        
        print("\nLost Pairs:")
        for m, s in sorted(lost_pairs):
            print(f"  {m} > {s}")
            
        print("\nFound Pairs:")
        for m, s in sorted(found_pairs):
            print(f"  {m} > {s}")

    conn.close()
except Exception as e:
    print("Error:", e)
