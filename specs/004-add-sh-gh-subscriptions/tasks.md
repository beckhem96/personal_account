# Tasks: add-sh-gh-subscriptions

**Input**: Design documents from `/specs/004-add-sh-gh-subscriptions/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md

**Tests**: 본 프로젝트 헌법 v1.0.0(Core Principle I)에 따라, 모든 기능의 추가 및 수정에 앞서 테스트 코드를 선제 작성(TDD)해야 합니다. 테스트 태스크는 항상 구현 태스크보다 앞에 실행되어야 합니다.

**Organization**: 각 태스크는 사용자 스토리별로 그룹화되어 독립적인 검증 및 점진적 인수를 지원합니다.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 프로젝트 기초 환경 및 RestClient 공통 빈 설정

- [x] T001 로컬 개발 환경의 루트 폴더 내 `.env` 파일에 `MYHOME_API_KEY` 환경 변수가 정의되어 있는지 확인 [.env]
- [x] T002 [P] RestClientConfig에 마이홈 API 전용 `myHomeRestClient` 빈 설정을 추가 [backend/src/main/java/org/example/account/config/RestClientConfig.java]

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: TDD 작성을 위한 테스트 클래스 및 mock 객체 환경 준비

- [x] T003 [P] 마이홈 API XML 응답 데이터의 테스트 더미 객체(Mock JSON/XML Node)를 생성하고 TDD 단위 테스트 작성을 위한 테스트 클래스 골격 준비 [backend/src/test/java/org/example/account/service/MyHomeSubscriptionServiceTest.java]

---

## Phase 3: User Story 1 - SH(서울주택도시공사) 청약 공고 조회 및 필터링 (Priority: P1) 🎯 MVP

**Goal**: 국토교통부 마이홈 API를 활용하여 SH 분양/임대 일정을 정상 필터링하여 노출한다.

**Independent Test**: `curl -s http://localhost:8080/api/subscriptions/sh/today` 로 SH 공고 데이터가 올바르게 파싱되어 반환되는지 확인한다.

### Tests for User Story 1 (MANDATORY - TDD required) ⚠️

- [x] T004 [P] [US1] 마이홈 API 응답 리스트에서 공급기관명이 "서울주택도시공사"인 것을 SH 공고로 분류하고, 타겟 지역 필터가 올바르게 작동하는지 확인하는 단위 테스트 케이스 선제 작성 및 실패 확인 [backend/src/test/java/org/example/account/service/MyHomeSubscriptionServiceTest.java]

### Implementation for User Story 1

- [x] T005 [US1] `MyHomeClient.java`를 신설하여 임대/분양 마이홈 API 호출 및 결과 리스트 병합 반환 기능 구현 [backend/src/main/java/org/example/account/client/MyHomeClient.java]
- [x] T006 [US1] `MyHomeSubscriptionService.java`를 신설하여 수집된 마이홈 데이터 중 공급기관명이 "서울주택도시공사"이고 타겟 지역에 매칭되는 항목을 선별하는 비즈니스 로직 구현 [backend/src/main/java/org/example/account/service/MyHomeSubscriptionService.java]
- [x] T007 [US1] `SubscriptionController.java`에 `@GetMapping("/sh/today")` 엔드포인트를 추가하고 `MyHomeSubscriptionService`를 주입받아 연결 [backend/src/main/java/org/example/account/controller/SubscriptionController.java]
- [x] T008 [P] [US1] 프론트엔드 API 호출 서비스인 `services.ts`에 `getShSubscriptions()` API 호출 기능 추가 [frontend/src/api/services.ts]
- [x] T009 [US1] 프론트엔드 `Subscriptions.tsx`에 'SH 공공' 탭 버튼을 신설하고 데이터 렌더링 카드 연동 [frontend/src/pages/Subscriptions.tsx]

---

## Phase 4: User Story 2 - GH(경기주택도시공사) 청약 공고 조회 및 필터링 (Priority: P2)

**Goal**: 국토교통부 마이홈 API를 활용하여 GH 분양/임대 일정을 정상 필터링하여 노출한다.

**Independent Test**: `curl -s http://localhost:8080/api/subscriptions/gh/today` 로 GH 공고 데이터가 올바르게 파싱되어 반환되는지 확인한다.

### Tests for User Story 2 (MANDATORY - TDD required) ⚠️

- [x] T010 [P] [US2] 마이홈 API 응답 리스트에서 공급기관명이 "경기주택도시공사"인 것을 GH 공고로 분류하고, 타겟 지역 필터가 올바르게 작동하는지 확인하는 단위 테스트 케이스 선제 작성 및 실패 확인 [backend/src/test/java/org/example/account/service/MyHomeSubscriptionServiceTest.java]

### Implementation for User Story 2

- [x] T011 [US2] `MyHomeSubscriptionService.java` 내에 공급기관명이 "경기주택도시공사"이고 타겟 지역에 매칭되는 항목을 선별하는 비즈니스 로직 구현 [backend/src/main/java/org/example/account/service/MyHomeSubscriptionService.java]
- [x] T012 [US2] `SubscriptionController.java`에 `@GetMapping("/gh/today")` 엔드포인트를 추가하고 `MyHomeSubscriptionService`를 주입받아 연결 [backend/src/main/java/org/example/account/controller/SubscriptionController.java]
- [x] T013 [P] [US2] 프론트엔드 API 호출 서비스인 `services.ts`에 `getGhSubscriptions()` API 호출 기능 추가 [frontend/src/api/services.ts]
- [x] T014 [US2] 프론트엔드 `Subscriptions.tsx`에 'GH 공공' 탭 버튼을 신설하고 데이터 렌더링 카드 연동 [frontend/src/pages/Subscriptions.tsx]

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: 전체 품질 보증, 성능 최적화 및 린트/빌드 검사

- [x] T015 전체 백엔드 컴파일 및 JUnit 테스트가 안전하게 빌드 통과하는지 검증 [backend]
- [x] T016 전체 프론트엔드 TypeScript 린트 에러 및 프로덕션 빌드 통과 여부 검증 [frontend]
- [x] T017 [P] Quickstart 가이드의 수동 테스트 시나리오 수행 완료 및 문서화 [specs/004-add-sh-gh-subscriptions/quickstart.md]

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존성 없음.
- **Foundational (Phase 2)**: Setup 완료 후 수행 가능. 테스트 스텁 기반을 마련.
- **User Stories (Phase 3~4)**: Foundational 완료 후 개별적으로 구현 가능.
  - Phase 3 (US1) -> Phase 4 (US2) 순서로 순차 개발을 진행함.
- **Polish (Phase 5)**: 모든 사용자 스토리가 완료된 후 진행 가능.

### Within Each User Story
- 테스트 코드(TDD) 작성 및 실패 확인 -> 클라이언트/서비스 비즈니스 레이어 구현 -> 컨트롤러 엔드포인트 연동 -> 프론트엔드 UI 완성 순서로 엄격하게 개발을 완료합니다.

---

## Parallel Opportunities

- Setup 태스크 중 `T002`는 병렬적으로 셋업할 수 있습니다.
- `US1`과 `US2`의 프론트엔드 API 추가(`T008`, `T013`)는 사전에 독립 정의할 수 있습니다.
- 백엔드 비즈니스 서비스 테스트 작성 및 API 수동 시나리오 보강은 동시에 실행 가능합니다.

---

## Implementation Strategy

### MVP First (User Story 1 우선 반영)
1. **Phase 1 & 2**를 진행하여 마이홈 API 데이터의 기초 셋업과 TDD 환경을 마련한다.
2. **Phase 3**를 완료하여 SH 공사의 모집 공고를 대시보드에 정상 표출시킨다.
3. MVP 단계 완료 후, **Phase 4**의 GH 공사 모집 공고 탭을 추가 확장 반영한다.
