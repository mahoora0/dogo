import os
from playwright.sync_api import sync_playwright

def main():
    # 저장될 비디오 디렉토리 설정
    os.makedirs('output/videos', exist_ok=True)
    
    with sync_playwright() as p:
        # 브라우저 실행
        browser = p.chromium.launch(headless=False)
        context = browser.new_context(
            record_video_dir="output/videos/",
            record_video_size={"width": 1920, "height": 1080},
            viewport={"width": 1920, "height": 1080}
        )
        page = context.new_page()
        page.on("dialog", lambda dialog: dialog.accept())

        # ==========================================
        # 모킹(Mocking) 설정
        # ==========================================
        # 닉네임 중복확인 모킹 (항상 통과)
        page.route("**/api/user/check-nickname*", lambda route: route.fulfill(status=200, json=False))
        # 이메일 전송 모킹
        page.route("**/api/mail/send", lambda route: route.fulfill(status=200, body="인증 메일이 발송되었습니다."))
        # 이메일 인증 확인 모킹
        page.route("**/api/mail/verify-for-join", lambda route: route.fulfill(status=200, json={"verificationToken": "mock-token"}))

        print("1. 시연 시작: 메인 페이지 이동")
        page.goto("http://localhost:8080")
        page.wait_for_timeout(2000)
        
        # 스크롤 
        page.mouse.wheel(0, 500)
        page.wait_for_timeout(1000)
        page.mouse.wheel(0, -500)
        page.wait_for_timeout(1000)

        # ==========================================
        # 시나리오 1: 회원가입
        # ==========================================
        print("2. 회원가입 진행")
        page.goto("http://localhost:8080/join")
        page.wait_for_timeout(1500)
        
        page.fill("input[name='nickname']", "시연유저")
        page.click("button:has-text('중복 확인')")
        page.wait_for_timeout(1000)
        
        page.fill("input[name='loginId']", "demouser")
        page.wait_for_timeout(500)
        
        page.fill("input[name='password']", "demo1234!")
        page.wait_for_timeout(500)
        
        page.fill("input[name='passwordConfirm']", "demo1234!")
        page.click("button:has-text('일치 확인')")
        page.wait_for_timeout(1000)
        
        page.fill("input[name='email']", "demo@test.com")
        page.click("button:has-text('인증 요청')")
        page.wait_for_timeout(1500)
        
        # SweetAlert 팝업 닫기 (이메일 발송 완료 알림)
        if page.is_visible(".swal2-confirm"):
            page.click(".swal2-confirm")
            page.wait_for_timeout(500)
        elif page.is_visible("text='확인'"):
            page.click("text='확인'")
            page.wait_for_timeout(500)
        
        page.fill("#verificationCode", "123456")
        page.click("button:has-text('인증 확인')")
        page.wait_for_timeout(1500)
        
        # SweetAlert 팝업 닫기 (인증 완료 알림이 있을 수 있음)
        if page.is_visible(".swal2-confirm"):
            page.click(".swal2-confirm")
            page.wait_for_timeout(500)
        
        # 가입 완료
        page.click("button:has-text('회원가입 완료')")
        page.wait_for_timeout(2000)

        # ==========================================
        # 시나리오 2: 로그인
        # ==========================================
        print("3. 일반 유저 로그인")
        page.goto("http://localhost:8080/login")
        page.wait_for_timeout(1000)
        page.fill("input[name='loginId']", "demouser")
        page.fill("input[name='password']", "demo1234!")
        page.click("button:has-text('로그인')")
        page.wait_for_timeout(2000)

        # ==========================================
        # 시나리오 3: 게시판 글쓰기 (분실물 등록)
        # ==========================================
        print("4. 분실물 등록 진행")
        page.goto("http://localhost:8080/lost-items/new")
        page.wait_for_timeout(1500)
        
        page.fill("input[name='title']", "검정색 카드지갑을 잃어버렸어요")
        page.wait_for_timeout(500)
        
        page.fill("input[name='itemName']", "카드지갑")
        page.wait_for_timeout(500)
        
        page.select_option("select[name='categoryMain']", "지갑")
        page.wait_for_timeout(500)
        
        page.select_option("select[name='lostAreaProvince']", "서울특별시")
        page.wait_for_timeout(1000)
        page.select_option("select[name='lostAreaDistrict']", "강남구")
        page.wait_for_timeout(500)
        
        page.fill("input[name='lostAt']", "2026-07-03T09:00")
        page.wait_for_timeout(500)
        
        page.fill("input[name='lostPlace']", "강남역 11번 출구 앞")
        page.wait_for_timeout(500)
        
        page.select_option("select[name='colorName']", "검정")
        page.wait_for_timeout(500)
        
        page.fill("textarea[name='content']", "내부에 신분증과 카드가 여러 장 있습니다. 꼭 좀 찾아주세요 ㅠㅠ")
        page.wait_for_timeout(1000)
        
        page.click("button:has-text('등록하기')")
        page.wait_for_timeout(3000)
        
        # 목록에서 확인 (스크롤)
        page.mouse.wheel(0, 400)
        page.wait_for_timeout(2000)

        # 로그아웃
        print("5. 로그아웃 진행")
        page.goto("http://localhost:8080/logout")
        page.wait_for_timeout(1500)

        # ==========================================
        # 시나리오 4: 관리자 로그인 및 공지사항 등록
        # ==========================================
        print("6. 관리자 로그인 및 공지사항 작성")
        page.goto("http://localhost:8080/login")
        page.wait_for_timeout(1000)
        page.fill("input[name='loginId']", "admin")
        page.fill("input[name='password']", "admin1234")
        page.click("button:has-text('로그인')")
        page.wait_for_timeout(2000)
        
        # 관리자 페이지 진입
        page.goto("http://localhost:8080/admin")
        page.wait_for_timeout(2000)
        page.mouse.wheel(0, 500)
        page.wait_for_timeout(1000)
        
        # 공지사항 등록
        page.goto("http://localhost:8080/admin/notice/new")
        page.wait_for_timeout(1500)
        
        page.fill("input[name='title']", "중요 시스템 점검 안내")
        page.wait_for_timeout(500)
        page.select_option("select[name='category']", "시스템안내")
        page.wait_for_timeout(500)
        
        page.fill("textarea[name='content']", "안정적인 서비스 제공을 위해 새벽 2시부터 4시까지 점검이 있을 예정입니다.")
        page.wait_for_timeout(1000)
        
        page.click("button[type='submit']")
        page.wait_for_timeout(2000)
        
        # 메인 페이지에서 공지사항 확인
        print("7. 메인 페이지 반영 확인")
        page.goto("http://localhost:8080")
        page.wait_for_timeout(3000)

        print("✅ 시연 영상 녹화가 성공적으로 완료되었습니다!")
        # 컨텍스트 닫기 (비디오 저장 트리거)
        context.close()
        browser.close()

if __name__ == "__main__":
    main()
