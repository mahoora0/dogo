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

공공 API가 가끔 `502 Bad Gateway` 같은 일시 오류를 줄 수 있어서 요청은 기본 5회 재시도합니다. 값을 바꾸려면:

```bash
python scripts/police_seed_collector.py \
  --database dogo_seed \
  --days 30 \
  --retry-count 10 \
  --retry-sleep 3
```

대량 백필은 기본적으로 조기 종료하지 않습니다. 공공 API의 페이지 기준이 수집 중 바뀔 수 있어서, 중간에 이미 저장된 페이지가 보여도 뒤쪽에 신규 데이터가 남아 있을 수 있습니다.

짧은 일일 갱신에서 이미 저장된 페이지를 만나면 멈추고 싶을 때만 값을 지정합니다.

```bash
python scripts/police_seed_collector.py \
  --database dogo_seed \
  --days 1 \
  --duplicate-page-limit 2
```

조기 종료를 명시적으로 끄려면:

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

개발 DB를 비우고 seed 기준으로 새로 만들려면:

```bash
scripts/import_seed_to_dev.sh seed/dogo_seed_YYYYMMDD.sql.gz --reset-db
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
SEED_API_RETRY_COUNT          기본 5
SEED_API_RETRY_SLEEP_SECONDS  기본 2
SEED_DUPLICATE_PAGE_LIMIT     기본 0, 0이면 조기 종료 끔
DEV_DB_NAME                   import 대상 개발 DB, 기본 dogo
DEV_DB_PASSWORD               개발 DB 비밀번호
```
