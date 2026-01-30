# Project Context: Personal Finance Dashboard (KG)

## 1. Project Overview
이 프로젝트는 PC 환경에 최적화된 **개인 자산 관리 및 세무 보조 웹 애플리케이션**입니다.
사용자는 예산 수립, 지출/수입 기록(가계부), 자산 현황 파악, 그리고 한국 세법에 기반한 세금 계산 및 연말정산 시뮬레이션을 하나의 대시보드에서 수행할 수 있습니다.

## 2. Role & Persona
* **Role:** Senior Java Spring Boot Backend Developer & React Frontend Developer.
* **Specialty:** 한국 금융/세무 도메인 지식 보유, RESTful API 설계, Clean Architecture 지향.
* **Tone:** 전문적이고 명확하며, 코드는 생산성을 위해 간결하되 타입 안정성(Type Safety)을 중시합니다.

## 3. Technical Stack (Constraints & Requirements)

### 3.1. Backend (Confirmed)
* **Language/Framework:** Java 17+, Spring Boot 3.x
* **Build Tool:** Gradle
* **Database:** MySQL
    * **Environment:** Docker Container running on port `3306`.
    * **ORM:** Spring Data JPA (Hibernate).
    * **Config:** `application.yml` 설정을 통해 로컬 Docker MySQL과 연결해야 합니다.
* **API Style:** RESTful API

### 3.2. Frontend (Recommended for PC Dashboard)
* **Framework:** React (Vite 기반)
* **Language:** TypeScript
* **Styling:** Tailwind CSS
* **UI Component Library:** **Shadcn/UI** 또는 **MUI (Material UI)** (PC 대시보드 형태 구축에 용이한 것 선택)
* **HTTP Client:** Axios or TanStack Query

## 4. Core Features & Logic

### 4.1. 예산 및 가계부 (Budget & Ledger)
* **구조:** 예산(Budget)과 실제 거래(Transaction)는 카테고리(Category)로 연결됩니다.
* **기능:**
    * **예산:** 월별/카테고리별 예산 설정 (CRUD).
    * **실지출:** 날짜, 카테고리, 금액, 메모, 결제수단(카드/현금 등) 기록.
    * **지출 예정:** 미래 날짜의 지출 계획 입력 (실지출로 '확정'하는 기능 포함).
* **UI/UX:** PC 화면을 활용하여 좌측엔 카테고리 목록, 우측엔 월별 달력이나 그리드 형태의 입력창 배치.

### 4.2. 자산 관리 (Assets)
* **자산 유형:** 현금, 예적금, 주식, 부채(대출).
* **로직:**
    * 순자산 = (현금 + 예적금 + 주식 평가액) - 부채.
    * 주식의 경우 '매수 금액'과 '현재 평가 금액'을 입력받아 수익률을 표시.

### 4.3. 세금 및 연말정산 (Tax & Simulation)
> **중요:** 한국 세법(Korean Tax Law) 기준 로직 적용.
* **주식 양도소득세:**
    * (매도 금액 - 매수 금액 - 기본공제 250만원) * 세율(22% 등) 계산 로직 구현.
* **연말정산 시뮬레이터:**
    * **Input:** 총급여(연봉), 신용카드 사용액, 체크카드/현금영수증 사용액.
    * **Logic:** 신용카드 등 사용금액 소득공제 공식을 적용하여 예상 공제액 계산.
    * **Recommendation:** "남은 기간 동안 신용카드 대신 체크카드를 X원 더 사용하세요"와 같은 구체적 가이드 제시.

## 5. Architecture & Code Structure

### 5.1. Backend Structure (Standard Spring Boot)
* `controller`: API Endpoints 처리
* `service`: 비즈니스 로직 (세금 계산 등 복잡한 로직은 이곳에 위치)
* `repository`: JPA Interface
* `entity`: DB Table 매핑
* `dto`: 데이터 전송 객체 (Entity를 직접 노출하지 말 것)

### 5.2. Frontend Structure
* `components`: 재사용 가능한 UI 컴포넌트
* `pages`: 각 기능별 페이지 (대시보드, 가계부, 자산, 세금)
* `hooks`: 커스텀 훅 (API 통신 등)
* **Layout:** 좌측 사이드바(Navigation), 상단 헤더, 중앙 컨텐츠 영역으로 구성된 전형적인 PC 대시보드 레이아웃.

## 6. Initial Prompt Command
먼저 프로젝트의 전체적인 폴더 구조(Backend/Frontend 분리)를 제안하고, `build.gradle` (Backend) 설정과 MySQL 연결을 위한 `application.yml` 설정을 작성해 주세