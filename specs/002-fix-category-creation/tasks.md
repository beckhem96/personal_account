# Tasks: fix-category-creation

**Input**: Design documents from `/specs/002-fix-category-creation/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: 본 프로젝트 헌법 v1.0.0(Core Principle I)에 따라, 모든 기능의 추가 및 수정에 앞서 테스트 코드를 선제 작성(TDD)해야 합니다. 테스트 태스크는 항상 구현 태스크보다 앞에 실행되어야 합니다.

**Organization**: 각 태스크는 사용자 스토리별로 그룹화되어 독립적인 검증 및 점진적 인수를 지원합니다.

---

## Phase 1: Setup (Shared Infrastructure)
 
 **Purpose**: 개발 환경 구동 및 로컬 빌드 검증
 
- [-] T001 로컬 가동 스크립트를 사용하여 DB 및 서버를 기동하고 정상 동작 확인 [start.sh]
+ [x] T001 로컬 가동 스크립트를 사용하여 DB 및 서버를 기동하고 정상 동작 확인 [start.sh]
 
 ---
 
 ## Phase 2: Foundational (Blocking Prerequisites)
 
 **Purpose**: 테스트를 선행 구현하기 위한 테스트 클래스 골격 준비
 
- [-] T002 카테고리 서비스 및 API 관련 테스트 케이스 작성을 위한 기본 테스트 클래스 골격 작성 [backend/src/test/java/org/example/account/service/CategoryServiceTest.java]
+ [x] T002 카테고리 서비스 및 API 관련 테스트 케이스 작성을 위한 기본 테스트 클래스 골격 작성 [backend/src/test/java/org/example/account/service/CategoryServiceTest.java]
 
 ---
 
 ## Phase 3: User Story 1 - 카테고리 이름 필수 및 공백 검증 (Priority: P1) 🎯 MVP
 
 **Goal**: 카테고리 등록/수정 시 이름에 공백이나 빈 값을 지정할 수 없도록 강제한다.
 
 **Independent Test**: 카테고리 등록 모달에서 공백 문자를 입력하고 저장을 시도할 때 저장 실패 경고가 노출되며, API 요청 전송이 차단된다.
 
 ### Tests for User Story 1 (MANDATORY - TDD required) ⚠️
 
- [-] T003 [P] [US1] 카테고리 등록/수정 시 이름이 누락되거나 공백일 때 예외(IllegalArgumentException)가 발생하는 단위 테스트 케이스 선제 작성 [backend/src/test/java/org/example/account/service/CategoryServiceTest.java]
+ [x] T003 [P] [US1] 카테고리 등록/수정 시 이름이 누락되거나 공백일 때 예외(IllegalArgumentException)가 발생하는 단위 테스트 케이스 선제 작성 [backend/src/test/java/org/example/account/service/CategoryServiceTest.java]
 
 ### Implementation for User Story 1
 
- [-] T004 [US1] CategoryService의 등록 및 수정 비즈니스 메서드 내에 이름 트리밍(Trim) 공백 및 빈 값 검증 로직 구현 [backend/src/main/java/org/example/account/service/CategoryService.java]
- [-] T005 [US1] 설정 화면 카테고리 저장 및 수정 폼에 공백 트리밍 유효성 검사 추가 및 사용자 오류 경고 UI 적용 [frontend/src/pages/Settings.tsx]
+ [x] T004 [US1] CategoryService의 등록 및 수정 비즈니스 메서드 내에 이름 트리밍(Trim) 공백 및 빈 값 검증 로직 구현 [backend/src/main/java/org/example/account/service/CategoryService.java]
+ [x] T005 [US1] 설정 화면 카테고리 저장 및 수정 폼에 공백 트리밍 유효성 검사 추가 및 사용자 오류 경고 UI 적용 [frontend/src/pages/Settings.tsx]
 
 ---
 
 ## Phase 4: User Story 2 - 연말정산 분류 역직렬화 견고성 강화 (Priority: P1)
 
 **Goal**: `yearEndCategory`에 빈 문자열(`""`)이나 `null`이 전달되더라도 역직렬화 에러 없이 `NONE`으로 파싱되게 한다.
 
 **Independent Test**: `yearEndCategory: ""` 페이로드를 담아 POST 요청을 보낼 때 400 Json Parse 에러 없이 `200 OK`가 리턴되고 데이터베이스에는 `NONE`으로 저장된다.
 
 ### Tests for User Story 2 (MANDATORY - TDD required) ⚠️
 
- [-] T006 [P] [US2] JSON 페이로드 역직렬화 시 빈 문자열 및 null 값이 YearEndCategory.NONE으로 안전하게 매핑되는지 검사하는 Jackson 바인딩 테스트 선제 작성 [backend/src/test/java/org/example/account/service/CategoryServiceTest.java]
+ [x] T006 [P] [US2] JSON 페이로드 역직렬화 시 빈 문자열 및 null 값이 YearEndCategory.NONE으로 안전하게 매핑되는지 검사하는 Jackson 바인딩 테스트 선제 작성 [backend/src/test/java/org/example/account/service/CategoryServiceTest.java]
 
 ### Implementation for User Story 2
 
- [-] T007 [US2] YearEndCategory enum 내부에 @JsonCreator 어노테이션이 부착된 팩토리 메서드를 구현하여 빈 값이나 알 수 없는 입력을 NONE으로 가공하도록 수정 [backend/src/main/java/org/example/account/domain/YearEndCategory.java]
- [-] T008 [US2] 프론트엔드 API 호출 직전 yearEndCategory 값이 비어 있거나 부적절할 경우 'NONE'으로 사전에 기본값을 보정하여 요청을 전송하도록 개선 [frontend/src/pages/Settings.tsx]
+ [x] T007 [US2] YearEndCategory enum 내부에 @JsonCreator 어노테이션이 부착된 팩토리 메서드를 구현하여 빈 값이나 알 수 없는 입력을 NONE으로 가공하도록 수정 [backend/src/main/java/org/example/account/domain/YearEndCategory.java]
+ [x] T008 [US2] 프론트엔드 API 호출 직전 yearEndCategory 값이 비어 있거나 부적절할 경우 'NONE'으로 사전에 기본값을 보정하여 요청을 전송하도록 개선 [frontend/src/pages/Settings.tsx]
 
 ---
 
 ## Phase 5: User Story 3 - 지출 외 카테고리의 연말정산 매핑 방지 규칙 강제 (Priority: P2)
 
 **Goal**: 수입(`INCOME`) 및 이체(`TRANSFER`) 카테고리의 `yearEndCategory`는 무조건 `NONE`으로 저장한다.
 
 **Independent Test**: 수입 유형의 카테고리를 추가할 때 연말정산 분류를 강제로 덮어써서 최종적으로 `NONE`으로 저장되는지 검증한다.
 
 ### Tests for User Story 3 (MANDATORY - TDD required) ⚠️
 
- [-] T009 [P] [US3] 수입/이체 유형의 카테고리를 저장하거나 수정할 때 연말정산 분류 필드가 강제로 NONE으로 고정 저장되는지 검증하는 단위 테스트 케이스 선제 작성 [backend/src/test/java/org/example/account/service/CategoryServiceTest.java]
+ [x] T009 [P] [US3] 수입/이체 유형의 카테고리를 저장하거나 수정할 때 연말정산 분류 필드가 강제로 NONE으로 고정 저장되는지 검증하는 단위 테스트 케이스 선제 작성 [backend/src/test/java/org/example/account/service/CategoryServiceTest.java]
 
 ### Implementation for User Story 3
 
- [-] T010 [US3] CategoryService의 createCategory 및 updateCategory 비즈니스 로직에 type이 EXPENSE가 아닌 경우 yearEndCategory 값을 강제로 NONE으로 고정하여 DB에 영속화하는 로직 추가 [backend/src/main/java/org/example/account/service/CategoryService.java]
+ [x] T010 [US3] CategoryService의 createCategory 및 updateCategory 비즈니스 로직에 type이 EXPENSE가 아닌 경우 yearEndCategory 값을 강제로 NONE으로 고정하여 DB에 영속화하는 로직 추가 [backend/src/main/java/org/example/account/service/CategoryService.java]
 
 ---
 
 ## Phase 6: Polish & Cross-Cutting Concerns
 
 **Purpose**: 전체 품질 보증, 성능 최적화 및 린트/빌드 검사
 
- [-] T011 전체 백엔드 컴파일 및 JUnit 테스트 실행 완료 및 에러 확인 [backend]
- [-] T012 전체 프론트엔드 TypeScript 린트 에러 및 프로덕션 빌드 통과 여부 검증 [frontend]
- [-] T013 Quickstart 가이드의 수동 테스트 시나리오 수행 완료 및 문서화 [specs/002-fix-category-creation/quickstart.md]
+ [x] T011 전체 백엔드 컴파일 및 JUnit 테스트 실행 완료 및 에러 확인 [backend]
+ [x] T012 전체 프론트엔드 TypeScript 린트 에러 및 프로덕션 빌드 통과 여부 검증 [frontend]
+ [x] T013 Quickstart 가이드의 수동 테스트 시나리오 수행 완료 및 문서화 [specs/002-fix-category-creation/quickstart.md]

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존성 없음. 프로젝트 가동 상태 확인 후 즉시 시작 가능.
- **Foundational (Phase 2)**: Setup 완료 후 수행 가능. 테스트 작성 골격 제공.
- **User Stories (Phase 3~5)**: Foundational 완료 후 개별적으로 구현 가능.
  - Phase 3 (US1) -> Phase 4 (US2) -> Phase 5 (US3) 순서로 순차 개발을 진행함.
- **Polish (Phase 6)**: 모든 사용자 스토리가 완료된 후 진행 가능.

### Within Each User Story
- 테스트 코드(TDD) 작성 및 실패 확인 -> 모델/엔티티 설계 보완 -> 서비스/비즈니스 레이어 구현 -> 프론트엔드 UI/UX 완성 순서로 엄격하게 개발을 완료합니다.

### Parallel Opportunities
- Phase 2 공통 테스트 기반 작업 완료 이후, `US1`, `US2`, `US3`의 백엔드와 프론트엔드 구현을 병렬 진행할 수 있습니다.
- 백엔드 팩토리 메서드 구현(`YearEndCategory.java`)과 프론트엔드 폼 보정(`Settings.tsx`)은 서로 다른 파일이므로 독립적으로 작업하여 완료할 수 있습니다.

---

## Parallel Example: User Story 2 & 3
```bash
# Jackson 역직렬화 테스트와 수입 고정 테스트 병렬 실행:
Task: "JSON 페이로드 역직렬화 시 YearEndCategory.NONE 매핑 테스트 작성" -> CategoryServiceTest.java
Task: "수입/이체 카테고리의 NONE 고정 테스트 작성" -> CategoryServiceTest.java
```

---

## Implementation Strategy

### MVP First (User Story 1 & 2 우선 반영)
1. **Phase 1 & 2**를 진행하여 카테고리 관련 테스트를 선행할 수 있는 준비를 마친다.
2. **Phase 3 & 4**를 완료하여 이름 공백 오류 및 HttpMessageNotReadableException 역직렬화 오류를 일거에 해소한다.
3. MVP 단계 완료 후, **Phase 5**의 수입/이체 매핑 고정 비즈니스 룰을 추가 반영한다.

### Incremental Delivery
- 카테고리 추가/수정의 각 검증 단위로 JUnit 빌드 테스트를 통과시킨 후 커밋한다.
- 프론트엔드 `Settings.tsx` 개선 후 ESLint 및 build 검증을 통과하여 점진적으로 릴리즈 가능한 상태를 보장한다.
