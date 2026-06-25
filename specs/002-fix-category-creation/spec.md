# Feature Specification: fix-category-creation

**Feature Branch**: `002-fix-category-creation`

**Created**: 2026-06-25

**Status**: Draft

**Input**: User description: "설정 > 카테고리에서 추가 하려는데 에러나고 있는 상황. 분석해서 수정"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 카테고리 이름 필수 및 공백 검증 (Priority: P1)

사용자가 설정 화면에서 카테고리를 새로 추가하거나 수정할 때, 이름 입력란에 공백만 입력하거나 빈 상태로 저장할 수 없도록 보장한다.

**Why this priority**: 빈 이름의 카테고리가 생성되면 가계부 목록이나 차트 등의 데이터 시각화 화면에서 구분이 불가능하여 심각한 UI 손상과 데이터 왜곡을 유발하므로 P1으로 처리한다.

**Independent Test**: 카테고리 추가/수정 모달에서 이름을 비우거나 공백(`"   "`)만 입력하고 저장 버튼을 누르면, 화면에 "카테고리 이름을 입력해 주세요." 경고가 표시되고 API 요청이 발생하지 않는다.

**Acceptance Scenarios**:

1. **Given** 카테고리 등록 모달이 열려 있고, **When** 이름 입력란에 아무것도 입력하지 않고 "추가"를 누르면, **Then** "카테고리 이름을 입력해 주세요." 경고 메시지가 화면에 노출되고 저장에 실패한다.
2. **Given** 카테고리 수정 모달이 열려 있고, **When** 이름을 `"   "`(공백)으로 채운 뒤 "수정"을 누르면, **Then** 공백 문자가 트리밍(Trimming)되어 비어 있는 것으로 판정되고 저장에 실패한다.

---

### User Story 2 - 연말정산 분류 역직렬화 및 값 검증 견고성 강화 (Priority: P1)

프론트엔드 또는 외부 API 호출을 통해 카테고리 등록/수정 요청 시 `yearEndCategory`의 값으로 빈 문자열(`""`)이나 `null`이 전달되더라도 시스템이 중단되지 않고 기본값인 `NONE`으로 안전하게 가공하여 저장한다.

**Why this priority**: 현재 빈 문자열(`""`) 전송 시 백엔드의 `HttpMessageNotReadableException` 예외로 인해 카테고리 저장이 즉시 차단되므로, 이 예외를 해결하기 위해 필수적으로 해결해야 하는 P1 스토리이다.

**Independent Test**: `yearEndCategory` 필드의 값을 `""` 또는 `null`로 지정하여 `POST /api/categories`에 요청을 보냈을 때 백엔드가 성공적으로 `200 OK`를 응답하고, 데이터베이스에는 해당 필드가 `NONE` (혹은 DB Column 상 `NONE` / `NULL`)으로 저장된다.

**Acceptance Scenarios**:

1. **Given** 카테고리 등록 API가 준비되어 있을 때, **When** Payload `{ "name": "신규카테고리", "type": "EXPENSE", "yearEndCategory": "" }`를 전송하면, **Then** 예외 없이 카테고리가 등록되고 응답 객체의 `yearEndCategory` 값은 `"NONE"`으로 매핑된다.
2. **Given** 카테고리 등록 API가 준비되어 있을 때, **When** Payload `{ "name": "신규카테고리2", "type": "EXPENSE", "yearEndCategory": null }`를 전송하면, **Then** 예외 없이 카테고리가 등록되고 데이터베이스에는 기본값 `NONE`이 저장된다.

---

### User Story 3 - 지출 외 카테고리의 연말정산 매핑 방지 규칙 강제 (Priority: P2)

수입(`INCOME`) 및 이체(`TRANSFER`) 유형의 카테고리는 연말정산 신용카드 공제 매핑 대상이 아니므로, `yearEndCategory`가 항상 `NONE`으로만 지정되도록 제한한다.

**Why this priority**: 수입과 이체 거래는 카드 소득공제 환급 대상이 아니므로 비즈니스 로직 및 데이터 일관성 보호를 위해 P2로 강제한다.

**Independent Test**: 카테고리 생성 시 유형을 `INCOME`으로 설정하고 `yearEndCategory`를 `PUBLIC_TRANSPORT`로 보내더라도, 서버 저장 시 강제로 `NONE`으로 조정되어 저장된다.

**Acceptance Scenarios**:

1. **Given** 카테고리 등록 API가 호출될 때, **When** Payload `{ "name": "급여수입", "type": "INCOME", "yearEndCategory": "TRADITIONAL_MARKET" }`로 전송하면, **Then** 카테고리는 등록되나 저장된 엔티티의 `yearEndCategory`는 `"NONE"`으로 최종 갱신된다.

---

### Edge Cases

- **동일 이름 카테고리 등록 시도**: 이미 존재하는 카테고리 이름으로 등록하려는 경우, 백엔드는 중복 데이터 에러를 반환하고 프론트엔드는 "이미 존재하는 카테고리 이름입니다." 라는 명확한 사용자 메시지를 표시해야 한다.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 사용자는 카테고리 등록/수정 시 공백이나 빈 문자열을 이름으로 지정할 수 없다 (Trimming 필수).
- **FR-002**: 백엔드는 카테고리 등록/수정 API 수신 시 `yearEndCategory`가 누락되거나 빈 문자열(`""`)로 올 경우, 에러를 던지지 않고 기본 Enum 값인 `YearEndCategory.NONE`으로 해석하여 안전하게 처리해야 한다.
- **FR-003**: 백엔드는 `INCOME` 및 `TRANSFER` 타입의 카테고리 생성/수정 시 `yearEndCategory`를 항상 `YearEndCategory.NONE`으로 고정하여 저장해야 한다.
- **FR-004**: 카테고리 이름은 전체 시스템에서 유일해야 하며(Unique), 중복 시 프론트엔드 모달에 적절한 에러 메시지를 표시하여 사용자에게 인지시켜야 한다.

### Key Entities *(include if feature involves data)*

- **Category (카테고리)**:
  - `name`: 카테고리 명칭 (유니크, 필수, 공백 불가)
  - `type`: 거래 유형 (`INCOME`, `EXPENSE`, `TRANSFER`)
  - `yearEndCategory`: 연말정산 신용카드 공제 분류 매핑 (`NONE`, `TRADITIONAL_MARKET`, `PUBLIC_TRANSPORT`)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 빈 문자열(`""`) 또는 `null`의 `yearEndCategory` 파라미터를 담은 카테고리 요청 전송 시, 100%의 성공률로 예외 없이 카테고리가 등록 및 수정된다.
- **SC-002**: 카테고리 등록 모달에서 공백 문자를 입력하고 저장을 시도할 때 저장 실패 경고가 노출되며, 불필요한 API 요청 전송 횟수가 0건으로 통제된다.

## Assumptions

- **데이터 호환성**: 기존 데이터베이스에 `NULL`로 적재되어 있는 `year_end_category` 값들은 조회 시 백엔드 단에서 자동으로 `YearEndCategory.NONE`으로 매핑되어 내려가며, 기존 레코드에 손상을 주지 않는다.
- **API 디자인**: 카테고리 관련 CRUD API는 기존 `/api/categories` 엔드포인트를 그대로 유지하며, 페이로드의 JSON 스키마 필드명도 변경 없이 일관되게 활용한다.
