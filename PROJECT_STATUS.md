# 프로젝트 현황: 개인 자산 관리 대시보드

## 1. 프로젝트 개요
PC 환경에 최적화된 **개인 자산 관리 및 세무 보조 웹 애플리케이션**입니다.
예산 관리, 지출/수입 기록(가계부), 자산 현황 파악, 한국 세법 기반 세금 계산 및 연말정산 시뮬레이션을 하나의 대시보드에서 수행할 수 있습니다.

## 2. 시스템 아키텍처

### 백엔드
* **프레임워크:** Java 17+, Spring Boot 3.x
* **빌드 도구:** Gradle
* **데이터베이스:** MySQL (Docker), JPA (Hibernate)
* **API 스타일:** RESTful API
* **주요 모듈:**
    * `controller`: API 엔드포인트
    * `service`: 비즈니스 로직 (세금 계산 등)
    * `repository`: 데이터 접근
    * `domain`: JPA 엔티티
    * `dto`: 데이터 전송 객체

### 프론트엔드
* **프레임워크:** React (Vite) + TypeScript
* **스타일링:** Tailwind CSS
* **HTTP 클라이언트:** Axios
* **구조:**
    * `pages`: Dashboard, Budget, Assets, Tax
    * `components`: 재사용 가능한 UI 컴포넌트

## 3. 데이터베이스 스키마

### Transaction (거래 내역)
* `id`: PK
* `date`: 거래 날짜
* `amount`: 거래 금액
* `memo`: 메모
* `paymentMethod`: Enum (CARD, CASH, BANK_TRANSFER)
* `category_id`: FK → Category
* `card_id`: FK → Card (Nullable)
* `isConfirmed`: Boolean (예정/확정 구분)

### Budget (예산)
* `id`: PK
* `year`, `month`: 대상 기간
* `amount`: 예산 한도
* `category_id`: FK → Category

### Asset (자산)
* `id`: PK
* `type`: Enum (CASH, SAVINGS, STOCK, DEBT)
* `name`: 자산명
* `balance`: 현재 가치
* `purchasePrice`: 매수가 (주식용)

### Card (카드)
* `id`: PK
* `name`: 카드명
* `type`: Enum (CREDIT, CHECK)

### RecurringTransaction (고정 비용)
* `id`: PK
* `name`: 고정 비용명
* `amount`: 금액
* `dayOfMonth`: 결제일
* `paymentMethod`: Enum
* `card_id`: FK → Card
* `category_id`: FK → Category

### Category (카테고리)
* `id`: PK
* `name`: 카테고리명
* `type`: Enum (INCOME, EXPENSE, TRANSFER)

## 4. 구현 완료된 기능

### 4.1. 기본 기능
* **예산 관리:** 월별/카테고리별 예산 CRUD
* **거래 내역 기록:** 실지출 및 예정 지출 기록
* **자산 관리:** 현금, 주식, 예적금, 부채 추적
* **카드 관리:** 카드 등록 및 관리
* **세금 계산:** 주식 양도소득세, 연말정산 시뮬레이션

### 4.2. 고정 비용 → 소비 자동 추가 (신규)
* **기능:** 고정 비용(RecurringTransaction) 생성 시 해당 월의 Transaction 자동 생성
* **로직:**
    * 현재 월의 dayOfMonth 날짜로 Transaction 생성
    * 이미 지난 날짜면 `isConfirmed = true`, 미래 날짜면 `false`
    * memo에 "(고정비용)" 표시
* **파일:** `RecurringTransactionService.java`

### 4.3. 카드별 소비 내역 조회 (신규)
* **백엔드 API:**
    * `GET /api/transactions/by-card/{cardId}` - 카드별 전체 조회
    * `GET /api/transactions/by-card/{cardId}?startDate=&endDate=` - 카드별 기간 조회
* **프론트엔드:**
    * 카드 필터 드롭다운 추가
    * 날짜 범위 선택 기능 (Custom Range 체크박스 + date picker)
    * 필터 적용 시 현재 조건 요약 표시
* **파일:**
    * `TransactionRepository.java`
    * `TransactionService.java`
    * `TransactionController.java`
    * `frontend/src/api/services.ts`
    * `frontend/src/pages/Budget.tsx`

## 5. 다음 작업 (Roadmap)

### 5.1. 고정 비용 월별 자동 생성
* **목표:** 매월 초에 등록된 고정 비용들의 Transaction을 자동으로 생성
* **구현 방안:**
    * Spring Scheduler를 사용하여 매월 1일에 실행
    * 모든 RecurringTransaction을 조회하여 해당 월의 Transaction 생성
    * 중복 생성 방지 로직 필요

### 5.2. 소비/예산 → 연말정산 연동
* **목표:** 실제 거래 데이터를 활용한 연말정산 자동 시뮬레이션
* **구현 방안:**
    * Transaction을 연도/결제수단별로 집계 (신용카드, 체크카드, 현금영수증)
    * 집계 데이터를 Tax 계산 서비스에 자동 연동

### 5.3. 현금 흐름 → 자산 연동
* **목표:** Transaction 생성 시 Asset 잔액 자동 업데이트
* **구현 방안:**
    * Transaction 생성/확정 시 관련 Asset 잔액 차감
    * 이체(Transfer) 카테고리 처리 로직 추가
