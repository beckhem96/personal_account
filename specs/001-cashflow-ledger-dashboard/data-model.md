# Data Model Design: Simplified Cashflow Ledger & Budget Dashboard

본 문서에서는 가계부 단순화 및 현금흐름 대시보드 구현을 위해 사용되는 데이터 모델과 제약 사항을 정의한다.

## 1. 기존 엔티티 구조 및 비즈니스 제약

### A. Transaction (거래 내역)
- **용도**: 수입, 지출, 이체, 저축/투자, 보험 등의 가계부 거래 데이터를 저장한다.
- **제약사항**:
  - `amount` (금액): 반드시 `BigDecimal` 사용.
  - `externalRef` (외부 참조 키): 카드사 명세서 엑셀 파싱 시 중복 거래 방지를 위한 Natural Key 역할을 한다. DB `UNIQUE` 인덱스로 설정되어 중복 데이터가 들어가지 않도록 보호한다.
  - `asset` (출금 자산): 이체(TRANSFER) 및 "저축/투자" 카테고리의 거래일 경우 필수값 (`NOT NULL` 비즈니스 검증 적용).
  - `toAsset` (입금 자산): 이체(TRANSFER) 및 "저축/투자" 카테고리의 거래일 경우 필수값 (`NOT NULL` 비즈니스 검증 적용).
  - `isConfirmed` (확정 여부): 미확정 거래(엑셀 임포트 초기 상태 등)는 자산 잔액(Balance) 변경에 영향을 주지 않으며, 확정 시에만 자산 잔액이 변경된다.

### B. Budget (예산)
- **용도**: 카테고리별 월간 소비 한도를 설정한다.
- **구조**:
  - `year`: 연도 (Integer)
  - `month`: 월 (Integer)
  - `amount`: 예산 금액 (BigDecimal, 필수)
  - `category`: 대상 카테고리 (Category, Many-to-One, 필수)

### C. Asset (자산)
- **용도**: 계좌, 현금, 주식, 부채 등 사용자의 재화 상태를 보관한다.
- **구조**:
  - `type`: CASH, SAVINGS, STOCK, DEBT
  - `name`: 자산 명칭
  - `balance`: 잔액 (BigDecimal)
  - `isDefault`: 기본 결제 수단으로 사용할지 여부 (Boolean)

### D. Card (카드 정보)
- **용도**: 신용카드 및 체크카드 정보.
- **구조**:
  - `company`: CardCompany enum (HANA, SAMSUNG, HYUNDAI, SHINHAN, KB)
  - `name`: 카드 이름
  - `type`: CARD_TYPE enum (CREDIT, CHECK)

### E. Category (카테고리)
- **용도**: 거래 항목 분류.
- **구조**:
  - `name`: 카테고리 이름 (예: 식비, 소득, 저축/투자, 보험, 기타 등)
  - `type`: INCOME, EXPENSE, TRANSFER

---

## 2. 카드사 엑셀 업로드 중복 방지 Natural Key 설계
각 카드사별로 동일 거래가 중복 임포트되는 것을 차단하기 위해, 아래 포맷으로 Natural Key를 빌드하여 `Transaction.externalRef`에 바인딩한다.

- **하나카드**: `HANA:[yyyy-MM-dd]:[가맹점명]:[금액]:[할부개월수]`
- **신한카드**: `SHINHAN:[yyyy-MM-dd]:[가맹점명]:[금액]:[할부개월수]`
- **국민카드**: `KB:[yyyy-MM-dd]:[가맹점명]:[금액]:[할부개월수]`

*할부 거래인 경우:*
- 일시불 또는 할부개월이 1인 경우 할부개월수 항목은 `"1"`로 채운다.
- 다년/다월 할부의 경우 개별 월별 가계부 내역으로 변환되어 저장될 때 고유 식별을 위해 순차 번호가 추가된다.
  - 예: `[CARD_COMPANY]:[원거래일자]:[가맹점명]:[원금액]:[회차]/[총할부개월]`
