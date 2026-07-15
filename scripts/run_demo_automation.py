import os
import sys
import time
import shutil
from pathlib import Path
from playwright.sync_api import sync_playwright
import pymysql
import cv2
import numpy as np

# 프로젝트 루트 경로 및 임시 비디오 경로 설정
ROOT_DIR = Path(r"c:\workspace\dogo")
WALLET_IMAGE = str(ROOT_DIR / "wallet.png")
POODLE_IMAGE = str(ROOT_DIR / "poodle.png")
TEMP_VIDEO_DIR = r"C:\Users\it\.gemini\antigravity\brain\8c22edb2-5b4b-450a-8963-13e1d0e42787\videos_temp"
FINAL_VIDEO_DIR = str(ROOT_DIR / "output" / "videos")
FINAL_VIDEO_PATH = os.path.join(FINAL_VIDEO_DIR, "dogo_demo_full.mp4")

# .env 환경변수 로드
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

# 데이터베이스 정리 (매번 시연이 깨끗하게 시작되도록 테스트용 계정과 관련 데이터 삭제)
def clean_db():
    load_dotenv(ROOT_DIR / ".env")
    try:
        conn = pymysql.connect(
            host=os.getenv("DEV_DB_HOST", "127.0.0.1"),
            port=int(os.getenv("DEV_DB_PORT", "3306")),
            user=os.getenv("DEV_DB_USER", os.getenv("DB_USERNAME", "root")),
            password=os.getenv("DEV_DB_PASSWORD", os.getenv("DB_PASSWORD", "")),
            database=os.getenv("DEV_DB_NAME", "dogo"),
            charset="utf8mb4",
            autocommit=True
        )
        with conn:
            with conn.cursor() as cursor:
                # 외래키 검사 일시 해제
                cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
                
                # 1. 기존 테스트 유저 아이디 또는 이메일과 연관된 데이터 삭제
                cursor.execute("SELECT user_no FROM users WHERE login_id IN ('usera', 'userb', 'userA', 'userB', 'useralost', 'userbfound') OR email IN ('lost_user@dogo.com', 'found_user@dogo.com')")
                user_nos = [row[0] for row in cursor.fetchall()]
                
                if user_nos:
                    user_nos_str = ",".join(map(str, user_nos))
                    # 채팅 메시지 삭제
                    cursor.execute(f"DELETE FROM chat_message WHERE sender_no IN ({user_nos_str})")
                    # 채팅방 삭제
                    cursor.execute(f"DELETE FROM chat_room WHERE inquirer_no IN ({user_nos_str}) OR owner_no IN ({user_nos_str})")
                    # 이미지 매칭 알림 삭제
                    cursor.execute(f"DELETE FROM item_match")
                    
                    # 분실물 이미지 및 분실물 삭제
                    cursor.execute(f"SELECT lost_id FROM lost_item WHERE user_no IN ({user_nos_str})")
                    lost_ids = [row[0] for row in cursor.fetchall()]
                    if lost_ids:
                        lost_ids_str = ",".join(map(str, lost_ids))
                        cursor.execute(f"DELETE FROM lost_item_image WHERE lost_id IN ({lost_ids_str})")
                        cursor.execute(f"DELETE FROM lost_item WHERE lost_id IN ({lost_ids_str})")
                        
                    # 습득물 이미지 및 습득물 삭제
                    cursor.execute(f"SELECT found_id FROM found_item WHERE user_no IN ({user_nos_str})")
                    found_ids = [row[0] for row in cursor.fetchall()]
                    if found_ids:
                        found_ids_str = ",".join(map(str, found_ids))
                        cursor.execute(f"DELETE FROM found_item_image WHERE found_id IN ({found_ids_str})")
                        cursor.execute(f"DELETE FROM found_item WHERE found_id IN ({found_ids_str})")
                        
                    # 실종자 리포트 삭제
                    cursor.execute(f"DELETE FROM missing_person_report WHERE user_no IN ({user_nos_str})")
                    # 실종동물 리포트 삭제
                    cursor.execute(f"DELETE FROM animal_report WHERE user_no IN ({user_nos_str})")
                    # 1:1 문의 내역 삭제
                    cursor.execute(f"DELETE FROM inquiry WHERE user_no IN ({user_nos_str})")
                    # 유저 삭제
                    cursor.execute(f"DELETE FROM users WHERE user_no IN ({user_nos_str})")
                
                # 2. 경찰/외부 API 동기화 데이터 제거 (테스트 매칭 결과 노이즈 제거용)
                cursor.execute("DELETE FROM lost_item_image WHERE lost_id IN (SELECT lost_id FROM lost_item WHERE source_type = 'POLICE')")
                cursor.execute("DELETE FROM lost_item WHERE source_type = 'POLICE'")
                cursor.execute("DELETE FROM found_item_image WHERE found_id IN (SELECT found_id FROM found_item WHERE source_type = 'POLICE')")
                cursor.execute("DELETE FROM found_item WHERE source_type = 'POLICE'")
                cursor.execute("DELETE FROM animal_report WHERE source_type IN ('ANIMAL_LOSS_API', 'ANIMAL_PROTECTION_API')")
                cursor.execute("DELETE FROM missing_person_report WHERE source_type = 'PUBLIC_API'")

                # 테스트 공지사항 글 삭제
                cursor.execute("DELETE FROM notice WHERE title LIKE '%DOGO 서비스 시스템%'")
                # 긴급 알림 플래그 초기화
                cursor.execute("UPDATE missing_person_report SET status = 'CLOSED'")
                cursor.execute("DELETE FROM animal_report WHERE title LIKE '%갈색 푸들을%'")

                cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
                print("[INFO] Database cleaning completed successfully.")
    except Exception as e:
        print(f"[WARNING] Database cleaning failed: {e}")
        raise e

# SweetAlert 팝업 자동 확인 처리
def dismiss_swal_if_visible(page):
    try:
        # SweetAlert2의 확인 버튼 클릭
        if page.locator(".swal2-confirm").is_visible(timeout=500):
            page.locator(".swal2-confirm").click()
            page.wait_for_timeout(500)
    except Exception:
        pass

# 메일 인증 처리 모킹 라우팅 설정
def setup_mock_routes(page):
    # 닉네임 중복확인 모킹
    page.route("**/api/user/check-nickname*", lambda route: route.fulfill(status=200, json=False))
    # 이메일 전송 모킹
    page.route("**/api/mail/send", lambda route: route.fulfill(status=200, body="인증 메일이 발송되었습니다."))
    # 이메일 인증 확인 모킹
    page.route("**/api/mail/verify-for-join", lambda route: route.fulfill(status=200, json={"verificationToken": "mock-token"}))

# 두 비디오 파일을 5:5 비율로 하나의 비디오로 결합하는 함수
def stitch_videos(video_a_path, video_b_path, output_path):
    print(f"\n[INFO] Stitching videos side-by-side:")
    print(f"  Left Video  (User A): {video_a_path}")
    print(f"  Right Video (User B): {video_b_path}")
    print(f"  Output Path         : {output_path}")
    
    if not os.path.exists(video_a_path) or not os.path.exists(video_b_path):
        print("[ERROR] Stitching failed. Source videos missing.")
        return False
        
    cap_a = cv2.VideoCapture(video_a_path)
    cap_b = cv2.VideoCapture(video_b_path)
    
    fps_a = cap_a.get(cv2.CAP_PROP_FPS)
    fps_b = cap_b.get(cv2.CAP_PROP_FPS)
    fps = max(fps_a, fps_b, 25.0)
    if fps <= 0 or fps > 100:
        fps = 25.0
        
    target_w = 960
    target_h = 1080
    
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(output_path, fourcc, fps, (target_w * 2, target_h))
    
    if not out.isOpened():
        print("[ERROR] Could not open output video writer.")
        cap_a.release()
        cap_b.release()
        return False
        
    frame_count = 0
    last_frame_a = None
    last_frame_b = None
    
    while True:
        ret_a, frame_a = cap_a.read()
        ret_b, frame_b = cap_b.read()
        
        if not ret_a and not ret_b:
            break
            
        if ret_a:
            frame_a_res = cv2.resize(frame_a, (target_w, target_h))
            last_frame_a = frame_a_res
        else:
            frame_a_res = last_frame_a if last_frame_a is not None else np.zeros((target_h, target_w, 3), dtype=np.uint8)
            
        if ret_b:
            frame_b_res = cv2.resize(frame_b, (target_w, target_h))
            last_frame_b = frame_b_res
        else:
            frame_b_res = last_frame_b if last_frame_b is not None else np.zeros((target_h, target_w, 3), dtype=np.uint8)
            
        combined = np.hstack((frame_a_res, frame_b_res))
        out.write(combined)
        frame_count += 1
        
    cap_a.release()
    cap_b.release()
    out.release()
    print(f"[SUCCESS] Stitched {frame_count} frames into single video: {output_path}")
    return True

def main():
    # 데이터베이스 세정
    clean_db()
    
    # 임시 비디오 폴더 생성 및 정리
    if os.path.exists(TEMP_VIDEO_DIR):
        shutil.rmtree(TEMP_VIDEO_DIR)
    os.makedirs(TEMP_VIDEO_DIR, exist_ok=True)
    
    # 느리게 동작하는 slow_mo 설정 (단위: ms)
    SLOW_MO_MS = 1000 
    
    page_a = None
    page_b = None
    
    with sync_playwright() as p:
        try:
            print("\n==================================================")
            print("[START] DOGO Project Demonstration Automation Script")
            print("==================================================\n")
            
            # ------------------------------------------------
            # 좌/우 브라우저 실행 (5:5 분할 화면 구성)
            # ------------------------------------------------
            print("[INFO] Launching User A browser (Left side)...")
            browser_a = p.chromium.launch(headless=False, slow_mo=SLOW_MO_MS, args=['--window-position=0,0', '--window-size=960,1040'])
            context_a = browser_a.new_context(
                viewport={"width": 940, "height": 950},
                record_video_dir=TEMP_VIDEO_DIR,
                record_video_size={"width": 960, "height": 1080}
            )
            page_a = context_a.new_page()
            page_a.on("dialog", lambda dialog: dialog.accept())
            setup_mock_routes(page_a)
            
            print("[INFO] Launching User B browser (Right side)...")
            browser_b = p.chromium.launch(headless=False, slow_mo=SLOW_MO_MS, args=['--window-position=960,0', '--window-size=960,1040'])
            context_b = browser_b.new_context(
                viewport={"width": 940, "height": 950},
                record_video_dir=TEMP_VIDEO_DIR,
                record_video_size={"width": 960, "height": 1080}
            )
            page_b = context_b.new_page()
            page_b.on("dialog", lambda dialog: dialog.accept())
            setup_mock_routes(page_b)
            
            # 양쪽 브라우저 초기 메인화면 접속 (녹화 동기화 시점 확보)
            page_a.goto("http://localhost:8080")
            page_b.goto("http://localhost:8080")
            page_a.wait_for_timeout(2000)
            page_b.wait_for_timeout(2000)
            
            # ------------------------------------------------
            # [Part 1] 회원가입 및 로그인 (User A - 분실이)
            # ------------------------------------------------
            print("\n--- [Part 1] User A Signup & Login ---")
            page_a.click("a[href='/join']")
            page_a.wait_for_url("**/join")
            page_a.wait_for_timeout(1000)
            
            page_a.fill("#nickname", "분실이")
            page_a.click("button[onclick='checkNickname()']")
            page_a.wait_for_timeout(800)
            
            page_a.fill("#loginId", "usera")
            page_a.fill("#password", "Password123!")
            page_a.fill("#passwordConfirm", "Password123!")
            page_a.click("button[onclick='checkPasswordMatch()']")
            page_a.wait_for_timeout(800)
            
            page_a.fill("#email", "lost_user@dogo.com")
            page_a.click("#sendMailBtn")
            page_a.wait_for_timeout(1000)
            dismiss_swal_if_visible(page_a)
            
            page_a.fill("#verificationCode", "123456")
            page_a.click("#verifyMailBtn")
            page_a.wait_for_timeout(1000)
            dismiss_swal_if_visible(page_a)
            
            page_a.click("form[action='/join'] button[type='submit']")
            page_a.wait_for_url("**/login")
            page_a.wait_for_timeout(1000)
            dismiss_swal_if_visible(page_a)
            
            page_a.fill("input[name='loginId']", "usera")
            page_a.fill("input[name='password']", "Password123!")
            page_a.click("form[action='/loginProcess'] button[type='submit']")
            page_a.wait_for_url("http://localhost:8080/")
            page_a.wait_for_timeout(2000)
            print("[INFO] User A logged in successfully.")
            
            # ------------------------------------------------
            # [Part 2] 분실물 등록 및 검색 (User A)
            # ------------------------------------------------
            print("\n--- [Part 2] User A Lost Item Register & Search ---")
            # 분실물 검색 이동
            page_a.goto("http://localhost:8080/lost-items")
            page_a.wait_for_timeout(1500)
            
            # 검색 필터 조작
            page_a.select_option("select[name='category']", value="지갑")
            page_a.wait_for_timeout(1500) # 자동 submit됨
            
            page_a.select_option("select[name='area']", value="서울특별시")
            page_a.wait_for_timeout(1500) # 자동 submit됨
            
            # 필터 전체보기 복구
            page_a.select_option("select[name='category']", value="")
            page_a.wait_for_timeout(1500)
            page_a.select_option("select[name='area']", value="")
            page_a.wait_for_timeout(1500)
            
            # 분실물 등록 작성
            page_a.goto("http://localhost:8080/lost-items/new")
            page_a.wait_for_timeout(1000)
            
            page_a.fill("input[name='title']", "상암동 부근에서 검은색 프라다 가죽 지갑을 분실했습니다")
            page_a.fill("input[name='itemName']", "프라다 지갑")
            page_a.select_option("select[name='categoryMain']", label="지갑")
            page_a.select_option("select[name='lostAreaProvince']", label="서울특별시")
            page_a.wait_for_timeout(500)
            page_a.select_option("select[name='lostAreaDistrict']", label="마포구")
            page_a.fill("input[name='lostAt']", "2026-07-06T09:00")
            page_a.fill("input[name='lostPlace']", "상암동 디지털미디어시티역 근처")
            page_a.select_option("select[name='colorName']", label="검정")
            page_a.fill("textarea[name='content']", "겉면에 미세한 스크래치가 있고 카드 3장과 신분증이 들어있습니다.")
            page_a.set_input_files("input[id='imageInput']", WALLET_IMAGE)
            page_a.wait_for_timeout(1500)
            
            page_a.click("form[action='/lost-items'] button[type='submit']")
            page_a.wait_for_timeout(2500)
            print("[INFO] User A lost item registered.")
            
            # ------------------------------------------------
            # [Part 3] 습득물 등록 및 자동 매칭 알림 (User B - 습득이)
            # ------------------------------------------------
            print("\n--- [Part 3] User B Register Found Item & Matching Alarm ---")
            # User B 회원가입
            page_b.goto("http://localhost:8080/join")
            page_b.wait_for_timeout(1000)
            
            page_b.fill("#nickname", "습득이")
            page_b.click("button[onclick='checkNickname()']")
            page_b.wait_for_timeout(800)
            
            page_b.fill("#loginId", "userb")
            page_b.fill("#password", "Password123!")
            page_b.fill("#passwordConfirm", "Password123!")
            page_b.click("button[onclick='checkPasswordMatch()']")
            page_b.wait_for_timeout(800)
            
            page_b.fill("#email", "found_user@dogo.com")
            page_b.click("#sendMailBtn")
            page_b.wait_for_timeout(1000)
            dismiss_swal_if_visible(page_b)
            
            page_b.fill("#verificationCode", "123456")
            page_b.click("#verifyMailBtn")
            page_b.wait_for_timeout(1000)
            dismiss_swal_if_visible(page_b)
            
            page_b.click("form[action='/join'] button[type='submit']")
            page_b.wait_for_url("**/login")
            page_b.wait_for_timeout(1000)
            dismiss_swal_if_visible(page_b)
            
            # User B 로그인
            page_b.fill("input[name='loginId']", "userb")
            page_b.fill("input[name='password']", "Password123!")
            page_b.click("form[action='/loginProcess'] button[type='submit']")
            page_b.wait_for_url("http://localhost:8080/")
            page_b.wait_for_timeout(2000)
            print("[INFO] User B logged in.")
            
            # User B 습득물 작성
            page_b.goto("http://localhost:8080/found-items/new")
            page_b.wait_for_timeout(1000)
            
            page_b.fill("input[name='title']", "상암역 스타벅스 앞 벤치에서 프라다 지갑을 습득했습니다")
            page_b.fill("input[name='itemName']", "프라다 지갑")
            page_b.select_option("select[name='categoryMain']", label="지갑")
            page_b.select_option("select[name='foundAreaProvince']", label="서울특별시")
            page_b.wait_for_timeout(500)
            page_b.select_option("select[name='foundAreaDistrict']", label="마포구")
            page_b.fill("input[name='foundAt']", "2026-07-06T10:00")
            page_b.fill("input[name='foundPlace']", "상암역 스타벅스 앞 벤치")
            page_b.select_option("select[name='colorName']", label="검정")
            page_b.fill("textarea[name='content']", "검정 가죽 재질이며 내부에 신분증이 들어있습니다.")
            page_b.set_input_files("input[id='imageInput']", WALLET_IMAGE)
            page_b.wait_for_timeout(1500)
            
            # 제출 및 보관 안내 모달 수락
            page_b.click("#foundItemForm button[type='submit']")
            page_b.wait_for_timeout(1000)
            if page_b.locator("#keepNoticeConfirm").is_visible():
                page_b.click("#keepNoticeConfirm")
            page_b.wait_for_timeout(3500)
            print("[INFO] User B registered matching found item.")
            
            # User A (Left) 화면 포커스 및 알림 대기
            print("[INFO] Checking live matching notification for User A...")
            page_a.wait_for_timeout(3000)
            # SSE 또는 폴백 업데이트를 위해 새로고침 적용
            page_a.reload()
            page_a.wait_for_timeout(2000)
            
            # 알림 벨 아이콘 클릭 및 확인
            page_a.click("#notificationBtn")
            page_a.wait_for_timeout(2000)
            
            # 드롭다운에서 매칭 점수 항목 클릭하여 습득 상세페이지 이동
            page_a.click("#notificationDropdown a.flex >> nth=0")
            page_a.wait_for_timeout(2500)
            
            # ------------------------------------------------
            # [Part 4] 1:1 실시간 채팅 & 사진 전송 (User A & B)
            # ------------------------------------------------
            print("\n--- [Part 4] 1:1 Real-time STOMP Chat ---")
            # User A: 1:1 채팅하기 클릭
            page_a.click("form[action='/chat/room'] button[type='submit']")
            page_a.wait_for_timeout(2500)
            
            # User A: 메시지 전송
            page_a.fill("#message-input", "안녕하세요! 제가 잃어버린 지갑이랑 너무 똑같은데 혹시 안에 신분증 이름이 어떻게 되어있나요?")
            page_a.click("button.chat-send-btn")
            page_a.wait_for_timeout(2500)
            
            # User B: GNB 채팅 메뉴 진입
            page_b.click("a.chat-button")
            page_b.wait_for_timeout(2500)
            
            # 채팅방 클릭
            page_b.click(".chat-room-item")
            page_b.wait_for_timeout(2000)
            
            # User B: 텍스트 답변
            page_b.fill("#message-input", "네 확인해 드릴게요. 잠시만요.")
            page_b.click("button.chat-send-btn")
            page_b.wait_for_timeout(2000)
            
            # User B: 이미지 전송
            page_b.set_input_files("#file-input", WALLET_IMAGE)
            page_b.wait_for_timeout(2500)
            page_b.click("button.chat-send-btn")
            page_b.wait_for_timeout(3500)
            
            # User A: 제 지갑이 맞습니다 전송
            page_a.fill("#message-input", "제 지갑이 맞습니다! 찾아주셔서 정말 감사합니다.")
            page_a.click("button.chat-send-btn")
            page_a.wait_for_timeout(3000)
            
            # ------------------------------------------------
            # [Part 5] 실종자 제보 및 실종동물 제보 등록 (User A)
            # ------------------------------------------------
            print("\n--- [Part 5] Register Missing Person & Missing Animal ---")
            # 실종자 제보 등록
            page_a.goto("http://localhost:8080/missing-persons/new")
            page_a.wait_for_timeout(1500)
            page_a.fill("input[name='age']", "78")
            page_a.fill("input[name='nationality']", "대한민국")
            page_a.fill("input[name='occurredAt']", "2026-07-05T14:00")
            page_a.fill("input[name='occurredPlace']", "상암 공원 근처")
            page_a.fill("input[name='heightCm']", "170")
            page_a.fill("input[name='weightKg']", "65")
            page_a.fill("input[name='bodyType']", "보통 체형")
            page_a.fill("input[name='faceShape']", "둥근형")
            page_a.fill("input[name='hairColor']", "회색")
            page_a.fill("input[name='hairStyle']", "짧은 백발 머리")
            page_a.fill("textarea[name='clothing']", "회색 바람막이와 베이지색 바지 착용")
            page_a.set_input_files("input[id='imageInput']", POODLE_IMAGE)
            page_a.wait_for_timeout(1500)
            page_a.click("form[action='/missing-persons'] button[type='submit']")
            page_a.wait_for_timeout(2500)
            
            # 실종동물 제보 등록
            page_a.goto("http://localhost:8080/animal-reports/new")
            page_a.wait_for_timeout(1500)
            page_a.fill("input[name='title']", "마포구 상암동에서 갈색 푸들을 찾습니다")
            page_a.fill("input[id='eventDateTimeCombined']", "2026-07-05T15:00")
            page_a.select_option("select[name='regionName']", label="서울특별시")
            page_a.fill("input[name='detailPlace']", "상암 디지털공원")
            page_a.select_option("select[name='animalType']", label="개")
            page_a.fill("input[name='breedName']", "푸들")
            page_a.select_option("select[name='gender']", label="암컷")
            page_a.select_option("select[name='neuteredStatus']", label="중성화 완료")
            page_a.set_input_files("input[id='imageInput']", POODLE_IMAGE)
            page_a.wait_for_timeout(1500)
            page_a.click("form[action='/animal-reports'] button[type='submit']")
            page_a.wait_for_timeout(2500)
            
            # ------------------------------------------------
            # [Part 6] AI 이미지 유사도 검색 기능 (User A)
            # ------------------------------------------------
            print("\n--- [Part 6] AI Image Similarity Search ---")
            page_a.goto("http://localhost:8080/animal-reports")
            page_a.wait_for_timeout(1500)
            page_a.click("button[onclick='toggleImageSearch()']")
            page_a.wait_for_timeout(1500)
            page_a.set_input_files("#imageSearchPanel input[name='image']", POODLE_IMAGE)
            page_a.wait_for_timeout(1000)
            page_a.click("#imageSearchPanel button[type='submit']")
            page_a.wait_for_timeout(4000)
            page_a.mouse.wheel(0, 400)
            page_a.wait_for_timeout(2500)
            
            # ------------------------------------------------
            # [Part 7] 유실물센터 연동 및 카카오맵 기반 위치 조회 (User A)
            # ------------------------------------------------
            print("\n--- [Part 7] Public Lost/Found Center Kakao Map ---")
            page_a.goto("http://localhost:8080/areas/list")
            page_a.wait_for_timeout(2500)
            
            # 시/도 선택
            try:
                page_a.select_option("#areaSelect", label="서울특별시")
            except Exception:
                page_a.select_option("#areaSelect", label="서울")
            page_a.wait_for_timeout(1500)
            
            # 시/군/구 선택
            page_a.select_option("#subAreaSelect", label="마포구")
            page_a.wait_for_timeout(1500)
            
            # 읍/면/동 선택
            try:
                page_a.select_option("#neighborhoodSelect", label="상암동")
            except Exception:
                page_a.select_option("#neighborhoodSelect", label="서교동")
            page_a.wait_for_timeout(2500)
            
            # 키워드 검색
            page_a.fill("#centerSearch", "경찰서")
            page_a.click("button.btn-search-black")
            page_a.wait_for_timeout(2500)
            
            # 지도 카드 선택 및 상세조회
            page_a.click("#centerList >> .center-card >> nth=0")
            page_a.wait_for_timeout(3000)
            
            # ------------------------------------------------
            # [Part 8] 고객지원 게시판 시연 (User A)
            # ------------------------------------------------
            print("\n--- [Part 8] Customer Support FAQ, Guide & Inquiry ---")
            page_a.goto("http://localhost:8080/faq")
            page_a.wait_for_timeout(2000)
            page_a.goto("http://localhost:8080/guide")
            page_a.wait_for_timeout(2000)
            
            page_a.goto("http://localhost:8080/inquiry")
            page_a.wait_for_timeout(1500)
            page_a.click("a[href='/inquiry/new']")
            page_a.wait_for_timeout(1500)
            page_a.fill("input[name='title']", "매칭 알림 기능 건의사항")
            page_a.fill("textarea[name='content']", "알림이 울릴 때 카카오톡 알림톡으로도 공유될 수 있게 기능 건의드립니다.")
            page_a.wait_for_timeout(1000)
            page_a.click("form[action='/inquiry'] button[type='submit']")
            page_a.wait_for_timeout(2500)
            
            # User A 로그아웃
            page_a.click("form[action='/logout'] button[type='submit']")
            page_a.wait_for_timeout(1500)
            
            # ------------------------------------------------
            # [Part 9] 관리자 페이지 및 대시보드 통계 (Admin)
            # ------------------------------------------------
            print("\n--- [Part 9] Admin Login & Dashboard Stats ---")
            page_a.goto("http://localhost:8080/login")
            page_a.wait_for_timeout(1000)
            page_a.fill("input[name='loginId']", "admin")
            page_a.fill("input[name='password']", "admin1234")
            page_a.click("form[action='/loginProcess'] button[type='submit']")
            page_a.wait_for_timeout(2000)
            
            page_a.goto("http://localhost:8080/admin")
            page_a.wait_for_timeout(2500)
            page_a.mouse.wheel(0, 300)
            page_a.wait_for_timeout(1500)
            
            # 설정 가중치 토글 클릭 (위치/시간 필터)
            page_a.click("#locationWeightToggle + div")
            page_a.wait_for_timeout(1500)
            dismiss_swal_if_visible(page_a)
            
            # CSV 데이터 백업 진행
            page_a.click("a[href='/admin/backup/csv']")
            page_a.wait_for_timeout(2000)
            
            # 회원 관리 페이지 이동
            page_a.goto("http://localhost:8080/admin/users")
            page_a.wait_for_timeout(2000)
            
            # ------------------------------------------------
            # [Part 10] 관리자 공지사항 및 긴급 알림 팝업 연동 (Admin)
            # ------------------------------------------------
            print("\n--- [Part 10] Admin Notice & Emergency Modal ---")
            # 공지사항 작성
            page_a.goto("http://localhost:8080/admin/notice/new")
            page_a.wait_for_timeout(1500)
            page_a.fill("input[name='title']", "DOGO 서비스 시스템 정기점검 및 매칭 기능 패치 안내")
            page_a.select_option("select[name='category']", label="시스템안내")
            page_a.fill("textarea[name='content']", "안녕하세요. DOGO 운영팀입니다. 더 원활한 매칭을 위해 금주 정기점검을 실시합니다. 점검 시간 중에는 서비스가 원활하지 않을 수 있으니 이용에 참고바랍니다.")
            page_a.wait_for_timeout(1500)
            page_a.click("form[action='/admin/notice'] button[type='submit']")
            page_a.wait_for_timeout(2500)
            
            # 대시보드 복귀 후 긴급 알림 스위치 활성화
            page_a.goto("http://localhost:8080/admin")
            page_a.wait_for_timeout(1500)
            
            # 새로 추가한 Emergency Alert 스위치 클릭 활성화
            page_a.click("#emergencyAlertToggle + div")
            page_a.wait_for_timeout(2500)
            
            # 관리자 로그아웃
            page_a.click("form[action='/logout'] button[type='submit']")
            page_a.wait_for_timeout(1500)
            
            # 일반 유저(Guest) 메인 접속하여 긴급 경보 모달 확인
            page_a.goto("http://localhost:8080")
            page_a.wait_for_timeout(3500) # 모달 페이드인 애니메이션 대기
            
            # 상세 정보 확인하기 링크 클릭
            page_a.click("#emergencyDetailLink")
            page_a.wait_for_timeout(3000)
            
            # 홈으로 다시 이동
            page_a.goto("http://localhost:8080")
            page_a.wait_for_timeout(2000)
            
            # 오늘 하루 보지 않기 클릭하여 모달 정상 종료
            page_a.click("button[onclick='closeEmergencyModalForToday()']")
            page_a.wait_for_timeout(2000)
            
            # 공지사항 확인
            page_a.goto("http://localhost:8080/notice")
            page_a.wait_for_timeout(2000)
            
            # 신규 작성된 공지 클릭하여 내용 확인
            page_a.click("a[href^='/notice/'] >> nth=0")
            page_a.wait_for_timeout(4000)
            
            print("\n==================================================")
            print("[SUCCESS] All demonstration parts completed successfully!")
            print("==================================================\n")
            
        except Exception as e:
            print(f"[ERROR] Script execution failed: {e}")
            try:
                if page_a:
                    page_a.screenshot(path="output/screenshot_error_a.png")
                    print("Captured error screenshot of page A to output/screenshot_error_a.png")
            except Exception as se:
                print(f"Failed to capture page A screenshot: {se}")
            try:
                if page_b:
                    page_b.screenshot(path="output/screenshot_error_b.png")
                    print("Captured error screenshot of page B to output/screenshot_error_b.png")
            except Exception as se:
                print(f"Failed to capture page B screenshot: {se}")
            raise e
        finally:
            # 브라우저 컨텍스트 종료 (녹화된 비디오가 파일로 디스크에 쓰여짐)
            print("[INFO] Closing browser contexts and finalising video recordings...")
            
            # 비디오 파일 경로 추출을 위해 context 및 page 닫기 전에 경로 획득
            video_a_raw_path = page_a.video.path() if page_a and page_a.video else None
            video_b_raw_path = page_b.video.path() if page_b and page_b.video else None
            
            if page_a: page_a.close()
            if context_a: context_a.close()
            if browser_a: browser_a.close()
            
            if page_b: page_b.close()
            if context_b: context_b.close()
            if browser_b: browser_b.close()
            
            # 비디오 파일 결합 (Stitch)
            if video_a_raw_path and video_b_raw_path:
                print("[INFO] Playwright raw videos recorded. Merging side-by-side...")
                success = stitch_videos(video_a_raw_path, video_b_raw_path, FINAL_VIDEO_PATH)
                if success:
                    print(f"\n[INFO] Demonstration video saved at: {FINAL_VIDEO_PATH}\n")
                else:
                    print("[WARNING] Video stitching failed.")
            else:
                print("[WARNING] Could not locate recorded video paths.")
                
            # 임시 폴더 삭제
            try:
                shutil.rmtree(TEMP_VIDEO_DIR)
            except Exception:
                pass

if __name__ == "__main__":
    main()
