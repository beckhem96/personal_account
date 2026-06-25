# API Contract Specification

본 가계부 및 대시보드 리팩토링 기능에서 활용하고 제공하는 주요 REST API 규격이다.

## 1. 거래 내역 API (Transactions)

### A. 거래 내역 조회 (List Transactions)
- **메서드/경로**: `GET /api/transactions`
- **쿼리 파라미터**:
  - `startDate`: 시작일 (format: `yyyy-MM-dd`, 필수)
  - `endDate`: 종료일 (format: `yyyy-MM-dd`, 필수)
  - `paymentMethod`: 결제수단 (`CARD`, `CASH`, `BANK_TRANSFER`, 선택)
- **응답 (JSON Array)**:
  ```json
  [
    {
      "id": 1,
      "date": "2026-06-25",
      "amount": -15000,
      "memo": "맛있는 식사",
      "paymentMethod": "CARD",
      "categoryName": "식비",
      "categoryId": 3,
      "isConfirmed": true,
      "cardName": "하나 신용카드",
      "cardId": 1,
      "assetName": "하나은행 계좌",
      "assetId": 2,
      "toAssetName": null,
      "toAssetId": null
    }
  ]
  ```

### B. 거래 내역 등록 (Create Transaction)
- **메서드/경로**: `POST /api/transactions`
- **요청 Body**:
  ```json
  {
    "date": "2026-06-25",
    "amount": 250000,
    "memo": "적금 납입",
    "paymentMethod": "BANK_TRANSFER",
    "categoryId": 4,
    "isConfirmed": true,
    "cardId": null,
    "assetId": 2,
    "toAssetId": 5
  }
  ```
- **비즈니스 규칙 검증**:
  - 만약 카테고리가 `TRANSFER` 유형이거나 카테고리 명이 `"저축/투자"` 인 경우, `assetId`(From)와 `toAssetId`(To)는 필수 항목이다. 누락 시 `400 Bad Request` 에러를 반환하며 예외 메시지는 `"이체 또는 저축/투자 거래에는 출금 자산(From) 및 입금 자산(To)을 필수 선택해야 합니다."` 로 지정된다.
- **응답 (JSON)**: 등록 완료된 `TransactionResponse` 객체 반환.

---

## 2. 예산 API (Budgets)

### A. 카테고리별 예산 설정 (Set Budget)
- **메서드/경로**: `POST /api/budgets`
- **요청 Body**:
  ```json
  {
    "year": 2026,
    "month": 6,
    "categoryId": 3,
    "amount": 500000
  }
  ```
- **응답 (JSON)**:
  ```json
  {
    "id": 1,
    "year": 2026,
    "month": 6,
    "categoryId": 3,
    "categoryName": "식비",
    "amount": 500000
  }
  ```

### B. 월간 예산 목록 조회 (Get Monthly Budgets)
- **메서드/경로**: `GET /api/budgets`
- **쿼리 파라미터**:
  - `year`: 조회 연도 (Integer)
  - `month`: 조회 월 (Integer)
  *또는 기간 범위:*
  - `startDate`: 시작일 (`yyyy-MM-dd`)
  - `endDate`: 종료일 (`yyyy-MM-dd`)
- **응답 (JSON Array)**: 설정된 월간 예산 리스트 반환.

---

## 3. 명세서 엑셀 가져오기 API (Statements)

### A. 카드사 명세서 업로드 (Import Statement)
- **메서드/경로**: `POST /api/statements/import`
- **요청 Multipart Form**:
  - `cardId`: 해당 명세서의 주체인 카드 ID (Long, 필수)
  - `file`: 업로드할 카드사 제공 엑셀 파일 (.xlsx, 필수)
- **동작**:
  1. `cardId`에 등록된 카드의 `CardCompany` 정보를 찾는다.
  2. `HANA`, `SHINHAN`, `KB` 등의 등록된 파서를 이용해 엑셀의 헤더(날짜, 가맹점, 금액, 할부정보)를 자동 탐색하여 파싱한다.
  3. Natural Key 중복 체크를 적용하여 이미 등록된 `externalRef`를 가진 거래는 삽입을 건너뛴다(Skipped).
  4. Gemini 카테고리 매핑기를 연동하여 가맹점 이름을 기반으로 카테고리를 추론해 임시 저장한다.
- **응답 (JSON)**:
  ```json
  {
    "importedCount": 42,
    "skippedCount": 5,
    "failedCount": 0,
    "unclassifiedCount": 2,
    "summary": [
      {
        "date": "2026-06-20",
        "merchant": "스타벅스",
        "amount": 5400,
        "categoryName": "카페/간식",
        "installmentSeq": null,
        "installmentMonths": null
      }
    ]
  }
  ```
