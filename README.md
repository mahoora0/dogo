# DOGO

## Local Setup

### 1. 환경 변수 파일 생성

프로젝트 루트에 `.env` 파일을 만들고 MySQL 접속 정보를 입력합니다.

```env
DB_USERNAME=root
DB_PASSWORD=본인_mysql_비밀번호
```

### 2. DB와 테이블 생성

앱을 실행하면 `application.properties`의 MySQL 접속 정보로 `dogo` 데이터베이스에 연결합니다.

`dogo` 데이터베이스가 없으면 `createDatabaseIfNotExist=true` 옵션으로 먼저 생성하고, 이후 `src/main/resources/schema.sql`을 실행해 테이블을 생성합니다.

MySQL 계정에 `CREATE DATABASE`, `CREATE TABLE` 권한이 있어야 합니다.

### 3. 로컬 DB 초기화

로컬 테이블 구조가 최신 스키마와 맞지 않으면 아래 SQL로 로컬 개발 DB를 초기화한 뒤 앱을 다시 실행합니다.

주의: 로컬 DB 데이터가 삭제됩니다.

```sql
DROP DATABASE IF EXISTS dogo;
CREATE DATABASE dogo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
