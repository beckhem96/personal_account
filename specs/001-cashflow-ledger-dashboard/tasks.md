# Tasks: Simplified Cashflow Ledger & Budget Dashboard

**Input**: Design documents from `/specs/001-cashflow-ledger-dashboard/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/api-contracts.md, research.md, quickstart.md

**Tests**: 본 프로젝트 헌법 v1.0.0(Core Principle I)에 따라, 모든 기능의 추가 및 수정에 앞서 테스트 코드를 선제 작성(TDD)해야 합니다. 테스트 태스크는 항상 구현 태스크보다 앞에 실행되어야 합니다.

**Organization**: 각 태스크는 사용자 스토리별로 그룹화되어 독립적인 검증 및 점진적 인수를 지원합니다.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 개발 환경 구동 및 로컬 빌드 검증

- [ ] T001 로컬 가동 스크립트를 사용하여 DB 및 서버를 기동하고 정상 동작 확인 [start.sh]

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 사용자 스토리 개발을 위해 필요한 공통 비즈니스 제약 규칙 및 공통 서비스 리팩토링

- [ ] T002 이체/투자 거래 시 From/To 자산 입력 검증에 대한 백엔드 단위 테스트 작성 [backend/src/test/java/org/example/account/service/TransactionServiceTest.java]
- [ ] T003 이체 및 저축/투자 거래 시 From/To 자산이 누락되면 예외를 던지는 검증 구현 [backend/src/main/java/org/example/account/service/TransactionService.java]
- [ ] T004 엑셀 임포트 시 카드사별 동적 `externalRef` prefix 키 생성을 위한 단위 테스트 작성 [backend/src/test/java/org/example/account/service/StatementImportServiceTest.java]
- [ ] T005 엑셀 임포트 중복 방지 키 prefix를 `"HANA"` 고정에서 `card.getCompany().name()`으로 동적 변경 구현 [backend/src/main/java/org/example/account/service/StatementImportService.java]

---

## Phase 3: User Story 1 - 통합 현금흐름 대시보드 (Priority: P1) 🎯 MVP

**Goal**: 이번 달의 전체 자금 유출입 상태(소득, 지출, 이체, 저축/투자, 보험)를 분석해 시각적인 차트와 리스트 형태로 대시보드를 제공한다.

**Independent Test**: 대시보드 조회 시 이번 달 등록 거래의 결제수단 및 카테고리에 맞춰 소득, 지출, 이체 등 요약 항목과 통계 수치가 맞는지 검증한다.

### Tests for User Story 1 (MANDATORY - TDD required) ⚠️

- [ ] T006 [P] [US1] 이번 달 현금흐름 요약 통계 조회를 위한 Repository 및 Service 조회 테스트 케이스 작성 [backend/src/test/java/org/example/account/service/TransactionServiceTest.java]

### Implementation for User Story 1

- [ ] T007 [US1] 이번 달의 결제수단별/카테고리별 지출 및 소득 요약 통계 조회 쿼리 및 서비스 로직 구현 [backend/src/main/java/org/example/account/service/TransactionService.java]
- [ ] T008 [US1] 프론트엔드 대시보드(`frontend/src/pages/Dashboard.tsx`) 한글화 및 현금흐름 요약(유입, 유출, 순현금흐름) 카드 배치 [frontend/src/pages/Dashboard.tsx]
- [ ] T009 [US1] Recharts를 이용한 결제수단별(신용/체크/현금/이체) 지출 비중 원형 차트(PieChart) 시각화 및 최근 거래 리스트 연동 [frontend/src/pages/Dashboard.tsx]

---

## Phase 4: User Story 2 - 카테고리별 예산 대비 지출 모니터링 (Priority: P1)

**Goal**: 카테고리별 설정된 한 달 예산 대비 실제 지출액의 비율을 계산하여 진척도 바(Progress Bar) 및 경고 UI를 노출한다.

**Independent Test**: 카테고리 예산 설정 후 거래가 추가될 때 진척도가 변경되고, 80% 및 100% 초과 시 경고 색상이 적용되는지 확인한다.

### Tests for User Story 2 (MANDATORY - TDD required) ⚠️

- [ ] T010 [P] [US2] 예산 대비 실제 지출액 집계 비즈니스 로직에 대한 단위 테스트 작성 [backend/src/test/java/org/example/account/service/BudgetServiceTest.java]

### Implementation for User Story 2

- [ ] T011 [US2] 특정 월의 카테고리별 예산 설정 정보와 실제 지출액을 매핑하여 반환하는 API 구현 [backend/src/main/java/org/example/account/service/BudgetService.java]
- [ ] T012 [US2] 대시보드 페이지에 카테고리별 예산 대비 지출 비율을 표시하는 진척도(Progress Bar) UI 구현 (80% 이상 노란색, 100% 초과 빨간색 적용) [frontend/src/pages/Dashboard.tsx]
- [ ] T013 [US2] 기존 `Budget.tsx` 가계부 화면에서 거래 내역 관리 및 팝업 모달을 제거하여 오직 예산 수립/설정만 수행하는 전용 페이지로 리팩토링 [frontend/src/pages/Budget.tsx]

---

## Phase 5: User Story 3 - 수동 거래 입력 인터페이스 단순화 (Priority: P2)

**Goal**: 복잡한 입력 레이아웃을 하나의 간소화된 팝업 모달로 통합하고, 금액 필드에 간단한 수식(`+`, `-`, `*`, `/`) 입력을 가능하게 한다.

**Independent Test**: 수동 입력 창에서 금액에 수식(`15000+4500`) 입력 시 결과(`19500`)가 반영되고, 자산이동(이체) 선택 시 From/To 자산 입력란이 활성화되며 등록이 수행되는지 점검한다.

### Tests for User Story 3 (MANDATORY - TDD required) ⚠️

- [ ] T014 [P] [US3] 프론트엔드 수동 입력 수식 평가기(`evaluateExpr`) 엣지 케이스 단위 테스트 보강 [frontend/src/utils/index.test.ts]

### Implementation for User Story 3

- [ ] T015 [US3] 신규 가계부 거래 관리 페이지 컴포넌트 파일 생성 및 목록/필터 레이아웃 기본 구조 작성 [frontend/src/pages/Ledger.tsx]
- [ ] T016 [US3] 라우터에 `/ledger` 등록 및 Sidebar 네비게이션 메뉴에 '가계부 내역' 항목 한글화 추가 [frontend/src/App.tsx], [frontend/src/components/Layout.tsx]
- [ ] T017 [US3] `Ledger.tsx`에 수동 입력 단일 팝업 모달 폼 구현 (금액 수식 연산 실시간 출력, 이체/저축/투자 타입 시 From-To 자산 입력란 필수 분기 처리 포함) [frontend/src/pages/Ledger.tsx]
- [ ] T018 [US3] `Ledger.tsx`에 월별 거래 목록 필터(지급수단, 기간) 및 수정/삭제 API 연동 완료 [frontend/src/pages/Ledger.tsx]

---

## Phase 6: User Story 4 - 카드사 명세서 엑셀 가져오기 및 카테고리 자동 매핑 (Priority: P2)

**Goal**: 기존 하나카드 외에 신한카드, KB국민카드 엑셀 공식 양식을 파싱하여 대량 거래를 중복 없이 일괄 등록하고 카테고리를 자동 분류한다.

**Independent Test**: 신한/국민카드 명세서 엑셀 파일을 업로드했을 때, 헤더를 스캔하여 거래 내역을 정상 파싱 및 카테고리 자동 매핑하고 중복 거래는 건너뛰는지 검증한다.

### Tests for User Story 4 (MANDATORY - TDD required) ⚠️

- [ ] T019 [P] [US4] 신한카드 명세서 엑셀 파서(`ShinhanCardStatementParser`) 및 국민카드 명세서 엑셀 파서(`KBCardStatementParser`) 독립 단위 테스트 작성 [backend/src/test/java/org/example/account/statement/ShinhanCardStatementParserTest.java], [backend/src/test/java/org/example/account/statement/KBCardStatementParserTest.java]

### Implementation for User Story 4

- [ ] T020 [US4] `CardStatementParser` 인터페이스를 구현하는 `ShinhanCardStatementParser` 클래스 생성 및 헤더 키워드 매칭, 신한 전용 중복 방지 naturalKey 생성 로직 구현 [backend/src/main/java/org/example/account/statement/ShinhanCardStatementParser.java]
- [ ] T021 [US4] `CardStatementParser` 인터페이스를 구현하는 `KBCardStatementParser` 클래스 생성 및 헤더 키워드 매칭, KB 전용 중복 방지 naturalKey 생성 로직 구현 [backend/src/main/java/org/example/account/statement/KBCardStatementParser.java]
- [ ] T022 [US4] 명세서 가져오기 화면(`Statements.tsx`)에 신한/국민카드 지원 여부 드롭다운 및 한글 UI 개선 [frontend/src/pages/Statements.tsx]

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 전체 품질 보증, 성능 최적화 및 린트/빌드 검사

- [ ] T023 전체 백엔드 컴파일 및 JUnit 테스트 실행 완료 및 에러 확인 [backend]
- [ ] T024 전체 프론트엔드 TypeScript 린트 에러 및 프로덕션 빌드 통과 여부 검증 [frontend]
- [ ] T025 [P] Quickstart 가이드의 통합 시나리오 수동 테스트 수행 완료 및 문서화 [specs/001-cashflow-ledger-dashboard/quickstart.md]

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존성 없음. 프로젝트 실행 확인 후 즉시 시작 가능.
- **Foundational (Phase 2)**: Setup 완료 후 수행 가능. 다른 모든 사용자 스토리(Phase 3~6)의 필수 차단(Blocking) 전제 조건.
- **User Stories (Phase 3~6)**: Foundational 완료 후 개별적으로 구현 가능.
  - Phase 3 (US1) -> Phase 4 (US2) -> Phase 5 (US3) -> Phase 6 (US4) 순서로 순차 개발 권장 (MVP 릴리즈 고려).
- **Polish (Phase 7)**: 모든 사용자 스토리가 완료된 후 진행 가능.

### Within Each User Story
- 테스트 코드(TDD) 작성 및 실패 확인 -> 모델/엔티티 설계 보완 -> 서비스/비즈니스 레이어 구현 -> 컨트롤러/API 엔드포인트 연동 -> 프론트엔드 UI/UX 완성 순서로 엄격하게 개발을 완료합니다.

### Parallel Opportunities
- Phase 2 공통 기반 작업 완료 이후, `US1`(대시보드), `US2`(예산), `US3`(수동입력 가계부), `US4`(엑셀 가져오기)의 백엔드와 프론트엔드 구현을 병렬 진행할 수 있습니다.
- 백엔드 엑셀 파서 클래스 추가(`ShinhanCardStatementParser` vs `KBCardStatementParser`) 작업은 완전히 독립된 파일이므로 병렬 작업이 가능합니다.

---

## Parallel Example: User Story 4
```bash
# 신한카드 파서와 국민카드 파서 단위 테스트 병렬 실행 및 실패 검증:
Task: "신한카드 명세서 엑셀 파서 단위 테스트 작성" -> ShinhanCardStatementParserTest.java
Task: "국민카드 명세서 엑셀 파서 단위 테스트 작성" -> KBCardStatementParserTest.java

# 독립 파서 구현 병렬 실행:
Task: "ShinhanCardStatementParser 클래스 구현" -> ShinhanCardStatementParser.java
Task: "KBCardStatementParser 클래스 구현" -> KBCardStatementParser.java
```

---

## Implementation Strategy

### MVP First (User Story 1 & 2 우선 반영)
1. **Phase 1 & 2** (Setup & Foundational)를 완료하여 공통 제약조건 및 이체 검증 기반을 단단하게 구축한다.
2. **Phase 3 & 4** (통합 현금흐름 대시보드 및 예산 진척도 시각화)를 개발 및 검증하여 사용자가 가장 먼저 볼 수 있는 가치를 전달한다.
3. MVP 단계 완성 후, 실제 가계부 거래 관리 페이지와 엑셀 대량 업로드를 통합 연동한다.

### Incremental Delivery
- 백엔드 비즈니스 룰 및 엑셀 파서 구현 단위로 JUnit 빌드 테스트를 통과시킨 후 커밋한다.
- 프론트엔드 신규 화면(`Ledger.tsx`) 구현을 위한 컴포넌트 추가 후 ESLint 및 build 검증을 단계별로 통과하여 점진적으로 배포 가능한 상태를 유지한다.
