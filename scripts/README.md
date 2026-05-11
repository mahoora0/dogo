# Police Seed Scripts

경찰청 분실물 30일치 seed 데이터셋을 Spring 앱과 분리해서 만드는 스크립트입니다.

## 1. 의존성 설치

```bash
python3 -m venv .venv-seed
source .venv-seed/bin/activate
pip install -r scripts/requirements-seed.txt
```

## 2. dogo_seed 수집

```bash
export POLICE_LOST_ITEM_SERVICE_KEY='발급받은_서비스키'
export SEED_DB_PASSWORD='mysql_password'

python scripts/police_seed_collector.py \
  --database dogo_seed \
  --days 30
```

기본 동작:

- `dogo_seed` DB가 없으면 생성
- `src/main/resources/schema.sql` 적용
- 목록 API로 기간 내 분실물을 페이지 수집
- 신규 `ATC_ID`마다 상세 API 호출
- `LOST_ITEM`, `LOST_ITEM_IMAGE`에 저장
- 이미 저장된 `ATC_ID`는 스킵

처음부터 다시 만들려면:

```bash
python scripts/police_seed_collector.py \
  --database dogo_seed \
  --days 30 \
  --reset-police
```

중간에 끊겼으면 같은 명령을 다시 실행하면 됩니다. 이미 들어간 `ATC_ID`는 스킵합니다.

이미 저장된 페이지를 만나면 기본적으로 연속 2페이지까지 확인한 뒤 조기 종료합니다. 이 값을 바꾸려면:

```bash
python scripts/police_seed_collector.py \
  --database dogo_seed \
  --days 1 \
  --duplicate-page-limit 1
```

조기 종료를 끄려면:

```bash
python scripts/police_seed_collector.py \
  --database dogo_seed \
  --days 1 \
  --duplicate-page-limit 0
```

`--update-existing`을 쓰는 경우에는 기존 `ATC_ID`도 업데이트 대상이라 조기 종료가 자동으로 비활성화됩니다.

## 3. dump 생성

```bash
export SEED_DB_PASSWORD='mysql_password'
scripts/dump_seed.sh
```

기본 출력:

```text
seed/dogo_seed_YYYYMMDD.sql.gz
```

## 4. 개발 DB에 import

```bash
export DEV_DB_PASSWORD='mysql_password'
scripts/import_seed_to_dev.sh seed/dogo_seed_YYYYMMDD.sql.gz
```

개발 DB 이름을 바꾸려면:

```bash
DEV_DB_NAME=dogo scripts/import_seed_to_dev.sh seed/dogo_seed_YYYYMMDD.sql.gz
```

## 주요 환경변수

```text
POLICE_LOST_ITEM_SERVICE_KEY  경찰청 API 키
SEED_DB_NAME                  seed DB 이름, 기본 dogo_seed
SEED_DB_HOST                  기본 127.0.0.1
SEED_DB_PORT                  기본 3306
SEED_DB_USER                  기본 root
SEED_DB_PASSWORD              seed DB 비밀번호
SEED_LOOKBACK_DAYS            기본 30
SEED_NUM_ROWS                 기본 100
SEED_API_SLEEP_SECONDS        기본 0.05
SEED_DUPLICATE_PAGE_LIMIT     기본 2, 0이면 조기 종료 끔
DEV_DB_NAME                   import 대상 개발 DB, 기본 dogo
DEV_DB_PASSWORD               개발 DB 비밀번호
```
