# DOGO

## Local Setup

### 1. MySQL 데이터베이스 생성

MySQL에서 `dogo` 데이터베이스를 생성합니다.

```sql
CREATE DATABASE IF NOT EXISTS dogo
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

### 2. 환경 변수 파일 생성

프로젝트 루트에 `.env` 파일을 만들고 MySQL 접속 정보를 입력합니다.

```env
DB_USERNAME=root
DB_PASSWORD=본인_mysql_비밀번호
```
