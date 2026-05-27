# 3. 카드 명세서 엑셀 import

## 배경

매 거래를 수동 입력하는 부담을 줄이기 위해 카드사가 제공하는 월별 이용내역 엑셀(xlsx)을 업로드하면 한 해 분량 카드 소비를 일괄로 가계부에 반영한다.

카드사마다 엑셀 양식(헤더 위치, 컬럼 이름, 금액 표기, 할부 표기)이 모두 달라 파서는 카드사별로 분리되어야 한다. 첫 구현은 **하나카드** 한 곳이지만 이후 삼성/현대/신한/국민카드 등이 추가될 것을 가정해 전략 패턴으로 추상화.

## 사용자 결정 사항

| 항목 | 결정 |
|------|------|
| 1차 지원 카드사 | 하나카드 (HANA) |
| 파일 형식 | xlsx (Apache POI) |
| 카테고리 자동 분류 | Google Gemini (기존 `GeminiClient` 재사용) |
| 중복 처리 | `externalRef`(자연 키) 일치 시 스킵 |
| 할부 처리 | N개월 균등 분할 → 매월 결제일 거래 N건 생성 |

## 도메인/스키마 변경

- **`Card.company`** (`CardCompany` enum: HANA/SAMSUNG/HYUNDAI/SHINHAN/KB) — 파서 선택 키
- **`Transaction.externalRef`** (String, nullable) — 명세서의 자연 키 저장. 재import 시 중복 스킵에 사용 (예: `HANA:2026-04-15:STARBUCKS:5800:1`)
- DDL은 `spring.jpa.hibernate.ddl-auto: update`로 자동 반영

## 백엔드 구성 (`backend/src/main/java/org/example/account/`)

### 파서 추상화 (`statement/` 패키지)

```
CardStatementParser (interface)
  ├─ CardCompany company()
  └─ List<ParsedTransaction> parse(InputStream xlsx)

HanaCardStatementParser (@Component)  → 하나카드 구현
CardStatementParserRegistry (@Component) → Map<CardCompany, Parser>
ParsedTransaction (record) → date, merchant, amount, installmentMonths/Seq, naturalKey
```

**하나카드 파서 특징**:
- Apache POI `XSSFWorkbook`로 첫 시트 읽음
- **헤더 자동 탐지**: "이용일", "이용가맹점", "이용금액", "할부개월" 키워드로 컬럼 매칭 (고정 셀 좌표 X)
- 금액: `replaceAll("[^0-9.\\-]", "")` 후 `BigDecimal`
- 할부 표기 정규식: `3/12`, `할부 3개월`, `일시불`/`0` 모두 처리

### 오케스트레이션 (`service/StatementImportService.java`)

흐름:
1. `cardId` → `Card.company` → `Registry.get(company)`로 파서 획득
2. `parser.parse(file.getInputStream())` → 명세서 행 추출
3. **중복 필터**: `TransactionRepository.findExistingExternalRefs(...)`로 1회 조회
4. **카테고리 분류**: `GeminiCategoryClassifier`로 가맹점 일괄 분류 (1회 호출)
5. **할부 전개**: `installmentMonths > 1`이면 `amount/N` × N개월 생성, 마지막 회차에 잔액 보정
6. **저장**: 모두 `isConfirmed=false`, `paymentMethod=CARD`, `externalRef=naturalKey`로 `saveAll`
7. 결과 요약 반환: imported/skipped/failed/unclassified + 미리보기 N건

### Gemini 분류기 (`service/GeminiCategoryClassifier.java`)

- 기존 `client/GeminiClient`를 의존성 주입해 재사용
- 키 미설정(`gemini.api-key=""`) 시 빈 Map 반환 → "기타" 카테고리 폴백 (`StatementImportService`가 처리)
- 응답 파싱 실패/타임아웃 시 예외 전파 안 함 — import 자체는 성공해야 함

### Controller (`controller/StatementController.java`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/statements/supported` | 지원 카드사 목록 |
| POST | `/api/statements/import` | `multipart/form-data`: cardId + file |

`application.yml`에 `spring.servlet.multipart.max-file-size: 10MB` 추가.

## 프론트엔드

- **`frontend/src/pages/Statements.tsx`** — 카드 선택 dropdown + 드래그앤드롭 업로드 + 결과 미리보기 테이블
- **`frontend/src/pages/Assets.tsx`** — 카드 생성 폼에 카드사 select 추가 (`HANA`/`SAMSUNG`/`HYUNDAI`/`SHINHAN`/`KB`/`미지정`)
- 라우트 `/statements`, 사이드바 "명세서 가져오기" (lucide `Upload` 아이콘)

## 테스트

- `HanaCardStatementParserTest` (5건): 할부 정규식 (분수형/텍스트형/일시불), POI 샘플 엑셀 파싱, 헤더 없으면 예외
- `StatementImportServiceTest` (4건): 일시불 1건, 할부 3개월 분할, 잔액 보정, 메모 회차 표기

## 영향 받은 기존 파일

- `backend/build.gradle` — Apache POI 5.3.0 추가
- `backend/src/main/resources/application.yml` — multipart 한도
- `backend/src/main/java/org/example/account/domain/Card.java` — `company` 필드
- `backend/src/main/java/org/example/account/domain/Transaction.java` — `externalRef` 필드
- `backend/src/main/java/org/example/account/repository/TransactionRepository.java` — `findExistingExternalRefs`
- `backend/src/main/java/org/example/account/controller/CardController.java` — PUT 추가
- `backend/src/main/java/org/example/account/service/CardService.java` — `updateCard`
- `backend/src/main/java/org/example/account/dto/CardRequest.java` / `CardResponse.java` — `company` 노출
- `frontend/src/App.tsx`, `frontend/src/components/Layout.tsx` — 라우트/메뉴
- `frontend/src/api/services.ts` — `statementService`, `updateCard`, `importStatement`
- `frontend/src/types/index.ts` — `CardCompany`, `StatementImportResponse` 등

## 알려진 위험

- **하나카드 양식 변경**: 헤더 키워드 기반이라 작은 변경엔 견디지만 컬럼 이름이 바뀌면 깨짐. 테스트 샘플로 회귀 감지
- **Gemini 분류 신뢰도**: 모든 import 거래는 `isConfirmed=false`. Budget 페이지에서 사용자가 검토 후 확정
- **할부 중복 위험**: `externalRef`에 회차 번호 포함(`HANA:date:merchant:price:1/3`)해 회차 단위 키로 사용
- **음수 금액(취소/환불)**: 현재 단순 합산. 정확한 처리는 별도 INCOME 변환 필요 — TODO

## 사용 흐름

1. Assets > 카드 탭 → 새 카드 생성 시 카드사를 "하나카드"로 지정
2. `/statements` 페이지 → 카드 선택 → xlsx 업로드 → "가져오기 실행"
3. 결과: "신규 등록 N건 / 중복 스킵 M건" + 미리보기 표
4. Budget의 "예정 거래"에서 확인 후 확정
