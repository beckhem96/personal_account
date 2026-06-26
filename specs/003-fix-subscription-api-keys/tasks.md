# Tasks: fix-subscription-api-keys

**Input**: Design documents from `/specs/003-fix-subscription-api-keys/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md

**Tests**: 본 프로젝트 헌법 v1.0.0(Core Principle I)에 따라, 모든 기능의 추가 및 수정에 앞서 테스트 코드를 선제 작성(TDD)해야 합니다. 테스트 태스크는 항상 구현 태스크보다 앞에 실행되어야 합니다.

**Organization**: 각 태스크는 사용자 스토리별로 그룹화되어 독립적인 검증 및 점진적 인수를 지원합니다.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 로컬 실행 환경 및 .env 구성 검토

- [x] T001 로컬 개발 환경의 루트 폴더에 `.env` 파일이 존재하고 `APPLYHOME_API_KEY` 및 `LH_API_KEY` 가 정의되어 있는지 확인 [.env]

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: TDD 작성을 위한 테스트 클래스 및 설정 준비

- [x] T002 스프링 설정이 정상적으로 로드되는지 입증하기 위한 테스트 클래스 및 Property 검증 스텁 준비 [backend/src/test/java/org/example/account/service/SubscriptionServiceTest.java]

---

## Phase 3: User Story 1 - 로컬 실행 시 .env 파일 환경변수 로딩 보장 (Priority: P1) 🎯 MVP

**Goal**: Gradle `bootRun` 기동 시 `.env` 환경 변수 자동 로딩을 보장한다.

**Independent Test**: `./gradlew :backend:bootRun` 실행 후 `curl -s http://localhost:8080/api/subscriptions/today` 를 호출하여 `apiKeyConfigured` 가 `true` 인지 검증한다.

### Tests for User Story 1 (MANDATORY - TDD required) ⚠️

- [x] T003 [P] [US1] 환경 변수 `APPLYHOME_API_KEY` 와 `LH_API_KEY` 가 스프링 Property로 주입되어 `client.isAvailable()`이 `true`를 반환하는지 검증하는 TDD 테스트 케이스 선제 작성 및 실패 확인 [backend/src/test/java/org/example/account/service/SubscriptionServiceTest.java]

### Implementation for User Story 1

- [x] T004 [US1] `backend/build.gradle` 파일 내의 `bootRun` 설정에 `.env` 파일을 로드하여 `environment` 속성에 동적으로 바인딩하는 Gradle DSL 스크립트 작성 [backend/build.gradle]
- [x] T005 [P] [US1] 백엔드 단위 테스트를 구동하여 T003 테스트 케이스가 성공 상태(Green)로 빌드 통과하는지 검증 [backend]

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: 전체 품질 보증, 성능 최적화 및 린트/빌드 검사

- [x] T006 전체 백엔드 컴파일 및 JUnit 테스트가 안전하게 빌드 통과하는지 검증 [backend]
- [x] T007 브라우저(`http://localhost:5173/subscriptions`)에 접속하여 노란색 API 키 미설정 경고 카드가 노출되지 않는지 수동 확인 [frontend/src/pages/Subscriptions.tsx]
- [x] T008 [P] Quickstart 가이드의 수동 테스트 시나리오 수행 완료 및 문서화 [specs/003-fix-subscription-api-keys/quickstart.md]

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존성 없음. 로컬 환경 확인 후 즉시 시작.
- **Foundational (Phase 2)**: Setup 완료 후 수행 가능. 테스트 작성 골격 제공.
- **User Stories (Phase 3)**: Foundational 완료 후 진행 가능.
- **Polish (Phase 4)**: 모든 사용자 스토리가 완료된 후 진행 가능.

### Within Each User Story
- 테스트 코드(TDD) 작성 및 실패 확인 -> Gradle 설정 빌드 변경 -> 빌드 컴파일 검증 순서로 엄격하게 개발을 완료합니다.

---

## Parallel Example: User Story 1
```bash
# 환경 변수 로딩 단위 테스트 및 빌드 컴파일 체크:
Task: "환경 변수 Property 주입 JUnit 테스트 선제 작성" -> SubscriptionServiceTest.java
Task: "Quickstart 시나리오 가이드 보강" -> quickstart.md
```

---

## Implementation Strategy

### MVP First
1. **Phase 1 & 2**를 진행하여 환경 검토 및 TDD 기반 설계를 마친다.
2. **Phase 3**의 Gradle DSL 로드 설정을 반영하여 API 키 미설정 경고 문제를 일거에 해결한다.
3. **Phase 4**의 전체 빌드 및 프론트엔드 연동 상태 수동 검증을 거쳐 최종 릴리즈한다.
