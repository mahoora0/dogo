import mysql.connector

try:
    conn = mysql.connector.connect(
        host="localhost",
        user="root",
        password="1234",
        database="dogo"
    )
    cursor = conn.cursor()
    
    print("=== found_item source_type counts ===")
    cursor.execute("SELECT source_type, COUNT(*) FROM found_item GROUP BY source_type")
    for row in cursor.fetchall():
        print(row)
        
    print("\n=== lost_item source_type counts ===")
    cursor.execute("SELECT source_type, COUNT(*) FROM lost_item GROUP BY source_type")
    for row in cursor.fetchall():
        print(row)
        
    print("\n=== animal_report source_type counts ===")
    cursor.execute("SELECT source_type, COUNT(*) FROM animal_report GROUP BY source_type")
    for row in cursor.fetchall():
        print(row)
        
    conn.close()
except Exception as e:
    print(f"Error: {e}")
