# 4. 연말정산 내 거래 기반 자동 산출

## 배경

기존 Tax 페이지의 연말정산 시뮬레이터는 사용자가 총급여 / 신용카드 / 체크·현금 / 전통시장 / 대중교통 5개 필드를 일일이 입력해야 했다. 이미 가계부에 한 해 분량 거래가 등록돼 있는데도 그 값을 다시 옮겨 적어야 하는 비효율을 해결.

기존 "직접 입력" 모드는 그대로 (자료가 없거나 가정 시나리오 시뮬용), 새 탭은 (1) 연도 선택 (2) 그 해의 확정 거래에서 5개 값 자동 집계 → 폼에 채움 (3) 사용자 보정 (4) 기존 계산기 호출.

## 사용자 결정 사항

| 항목 | 결정 |
|------|------|
| 전통시장/대중교통 분류 | Category 엔티티에 `yearEndCategory` enum 매핑 필드 |
| 자동 산출값 수정 | 가능 (자동 산출 → 폼 채움 → 보정 → 계산) |

## 도메인/스키마 변경

### `YearEndCategory` enum
`backend/src/main/java/org/example/account/domain/YearEndCategory.java`
- `NONE` (기본, 분류 없음)
- `TRADITIONAL_MARKET` (전통시장)
- `PUBLIC_TRANSPORT` (대중교통)

신용카드/체크카드/현금은 `PaymentMethod`와 `Card.type`으로 이미 구분되므로 enum에 별도 항목 불필요.

### `Category` 엔티티
- `@Enumerated(EnumType.STRING) private YearEndCategory yearEndCategory` (nullable)
- `update(...)` 메서드 오버로드, `yearEndCategoryOrNone()` 헬퍼

## 백엔드 구성

### Repository
`TransactionRepository.findConfirmedWithJoinsBetween(start, end)` — Category/Card fetch join + `isConfirmed=true` 필터. N+1 회피.

```java
@Query("SELECT DISTINCT t FROM Transaction t " +
       "LEFT JOIN FETCH t.category LEFT JOIN FETCH t.card " +
       "WHERE t.date BETWEEN :start AND :end AND t.isConfirmed = true")
List<Transaction> findConfirmedWithJoinsBetween(LocalDate start, LocalDate end);
```

### Service
`TaxService.computeYearEndFromTransactions(int year) → YearEndSettlementRequest`

집계 규칙:
| 항목 | 집계 기준 |
|------|-----------|
| `totalSalary` | `TransactionType.INCOME` AND 카테고리명이 `"월급"` |
| `creditCardAmount` | `PaymentMethod=CARD` AND `card.type=CREDIT` AND `EXPENSE` |
| `debitCashAmount` | `(PaymentMethod=CARD AND card.type=CHECK)` 또는 `PaymentMethod=CASH`, EXPENSE |
| `traditionalMarketAmount` | `category.yearEndCategory=TRADITIONAL_MARKET`, EXPENSE |
| `publicTransportAmount` | `category.yearEndCategory=PUBLIC_TRANSPORT`, EXPENSE |

**단순화 정책**: 전통시장/대중교통 사용액을 신용/체크 합계에서 차감하지 **않는다**. 한국 실제 룰에서는 신용카드로 전통시장 결제 시 카드 합계에서 빼지만, 본 시뮬레이터는 시뮬용이며 사용자가 폼에서 보정 가능하므로 단순화 우선.

**카드 정보 없는 CARD 결제**: 신용/체크 구분 불가 → CASH로 간주 (체크/현금 합계로 합산).

### Controller
```
GET /api/tax/year-end/auto?year=2026 → YearEndSettlementRequest
```
계산까지 하지 않고 "폼에 채울 값"만 반환. 계산은 기존 `POST /api/tax/year-end` 그대로 사용.

### Category DTO 보강
`CategoryRequest`/`CategoryResponse`에 `yearEndCategory` 필드 노출. `CategoryService.create/update`에서 반영. PUT 엔드포인트는 이미 존재.

## 프론트엔드

### Tax 페이지 (`frontend/src/pages/Tax.tsx`)
- 탭 구조: `STOCK` / `YEAR_END_MANUAL` / `YEAR_END_AUTO` (3개)
- 결과 표시는 `YearEndResultPanel` 컴포넌트로 추출해 두 모드가 공유
- `YearEndAutoSimulator` 신규:
  - 연도 select (현재 연도 ±2)
  - "자동 산출" 버튼 → `getAutoYearEndSettlement(year)` → 5개 필드에 채움
  - 자동 산출 직후 안내: "'월급' 카테고리 합계로 계산됩니다. 필요시 보정하세요"
  - 5개 editable input (재사용 `YearEndInputForm`)
  - "공제액 계산" → 기존 `simulateYearEnd` 호출

### Settings 페이지 (`frontend/src/pages/Settings.tsx`)
- EXPENSE 카테고리 행 우측에 인라인 select ("매핑 없음" / "전통시장" / "대중교통")
- 변경 즉시 `updateCategory(id, ...)` 호출, 토스트/리프레시
- 카테고리 모달에도 EXPENSE 타입일 때 매핑 select 표시

### 타입/API
- `frontend/src/types/index.ts`: `YearEndCategory` 유니온, `Category.yearEndCategory`, `CategoryRequest.yearEndCategory`
- `frontend/src/api/services.ts`: `getAutoYearEndSettlement(year)`

## 테스트

`backend/src/test/java/org/example/account/service/TaxServiceTest.java` (4건, 모두 통과):
- 분류별 정확합산 (신용/체크/현금/전통시장/대중교통)
- 카드 없는 CARD 결제는 체크/현금에 합산
- 부수입 카테고리는 총급여에 포함 안 됨
- 데이터 없으면 모두 0

## 사용 흐름

1. **Settings**에서 "교통/차량" 카테고리 → "대중교통" 매핑, "전통시장" 카테고리 생성 후 매핑
2. **Tax** → "연말정산 (내 거래 기반)" 탭 → 연도 선택 → "자동 산출" 클릭
3. 5개 필드 자동 채워짐 + 안내 박스
4. 필요시 총급여를 실수령액 기준으로 보정
5. "공제액 계산" → 결과 패널에 항목별 공제액 + 최종 공제액 + 절세 가이드

## 알려진 위험

- **"월급" 카테고리 가정**: 총급여 산출이 카테고리 이름 `"월급"`에 강결합. 이름이 바뀌면 0. 폼이 수정 가능하므로 보정으로 우회
- **신용/체크 + 전통시장/대중교통 중복**: 위 단순화 정책 참조
- **확정되지 않은 거래 제외**: 명세서 import한 미확정 거래는 Budget에서 확정해야 반영됨. 안내로 명시

## 영향 받은 기존 파일

- `backend/src/main/java/org/example/account/domain/Category.java`
- `backend/src/main/java/org/example/account/dto/CategoryRequest.java` / `CategoryResponse.java`
- `backend/src/main/java/org/example/account/service/CategoryService.java`
- `backend/src/main/java/org/example/account/service/TaxService.java`
- `backend/src/main/java/org/example/account/controller/TaxController.java`
- `backend/src/main/java/org/example/account/repository/TransactionRepository.java`
- `frontend/src/pages/Tax.tsx`
- `frontend/src/pages/Settings.tsx`
- `frontend/src/types/index.ts`, `frontend/src/api/services.ts`
