# Feature Specification: fix-subscription-api-keys

**Feature Branch**: `003-fix-subscription-api-keys`

**Created**: 2026-06-25

**Status**: Draft

**Input**: User description: "@[.env] 파일에 APPLYHOME_API_KEY랑 LH_API_KEY 설정했는데 청약일정 탭에 들어가니까 설정 안되어 있다고 뜨네. 이것 좀 확인해줘."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 로컬 실행 시 .env 파일 환경변수 로딩 보장 (Priority: P1)

사용자가 `.env` 파일에 `APPLYHOME_API_KEY`, `LH_API_KEY`를 설정했을 때, 전체 시스템 기동 스크립트(`./start.sh`)를 실행하거나 에이전트 환경에서 백엔드를 개별적으로 구동할 때 해당 환경변수들이 누락 없이 스프링 부트 애플리케이션으로 주입되어야 한다.

**Why this priority**:
API 키가 정상적으로 로드되지 않으면 청약 정보를 공공 API에서 가져올 수 없으므로, 핵심 기능인 청약 일정 조회가 마비된다. 따라서 최우선순위(P1)로 해결해야 한다.

**Independent Test**:
프로젝트 루트의 `.env` 파일에 임의의 값을 정의하고 백엔드를 실행한 후, `curl http://localhost:8080/api/subscriptions/today`를 호출하여 응답 JSON 내 `apiKeyConfigured` 필드가 `true`로 리턴되는지 독립적으로 테스트한다.

**Acceptance Scenarios**:

1. **Given** `.env` 파일에 유효한 API Key들이 정의됨
   **When** `./start.sh` 스크립트를 사용해 시스템 전체를 기동함
   **Then** `http://localhost:5173/subscriptions` (청약 일정 탭)에서 "API 키가 설정되지 않았습니다" 라는 경고 카드가 노출되지 않고 정상적으로 목록이 조회된다.

2. **Given** `.env` 파일에 API Key가 주입된 상태에서 백엔드를 별도 백그라운드 프로세스로 구동함
   **When** `curl -s http://localhost:8080/api/subscriptions/today` 호출을 시도함
   **Then** 결과 JSON에서 `"apiKeyConfigured": true`가 출력된다.

---

### Edge Cases

- **에이전트 구동 터미널 환경 격리**: `./start.sh`를 통하지 않고 에이전트 실행 툴이나 IDE 콘솔에서 직접 `./gradlew bootRun`을 실행하는 경우, 호스트 환경 변수가 로드되지 않아 API 키가 누락될 수 있다. 백엔드 개별 기동 프로세스에도 `.env` 로딩 환경을 연계해야 한다.
- **API Key 특수기호 손실**: data.go.kr의 공공 API 키는 복잡한 인코딩 특수문자(`+`, `/`, `=`)를 대량 포함하고 있다. `.env` 파일을 쉘에서 파싱하여 로드하는 과정에서 이 특수문자들이 올바르게 유지되지 않거나 이스케이프 오류가 나지 않는지 확인해야 한다.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 백엔드 애플리케이션 구동 시, 프로젝트 루트의 `.env` 파일 내 정의된 모든 환경변수가 누락 없이 스프링 부트 프로세스(`bootRun` 등)에 주입되어야 한다.
- **FR-002**: `./start.sh` 스크립트는 `.env` 파일의 유무를 감지하고, 내부에 선언된 변수들을 쉘 환경에 `export` 상태로 정의한 후 백엔드 및 프론트엔드를 실행해야 한다.
- **FR-003**: 백그라운드 실행을 포함한 모든 실행 스크립트와 명령어에서 환경변수 로딩 동작의 일관성을 보장한다.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 브라우저에서 `http://localhost:5173/subscriptions` 페이지 로딩 시, 청약홈(APT)과 LH 공공 서브 탭 모두에서 "API 키가 설정되지 않았습니다" 노란색 경고 창이 사라진다.
- **SC-002**: `curl http://localhost:8080/api/subscriptions/today` 응답 결과의 `apiKeyConfigured` 값이 항상 `true`를 유지한다.
- **SC-003**: `curl http://localhost:8080/api/subscriptions/lh` 응답 결과의 `apiKeyConfigured` 값이 항상 `true`를 유지한다.

## Assumptions

- 사용자는 `.env` 파일에 정상적인 API 키 값을 정의해 두었다고 가정한다.
- 백엔드 자바 코드 내부의 `isAvailable()` 판정 로직은 이상이 없으며, 오직 실행 프로세스 수준의 환경변수 누락이 유일한 원인이라고 가정한다.
