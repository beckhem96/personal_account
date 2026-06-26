# Feature Specification: add-sh-gh-subscriptions

**Feature Branch**: `004-add-sh-gh-subscriptions`

**Created**: 2026-06-26

**Status**: Draft

**Input**: User description: "현재 청약 일정 탭에 LH랑 청약홈 일정은 가져오는데 SH, GH 정보 가져오는 기능이 없어. 기능 추가해줘. 하는데 필요한 KEY는 나한테 요구하고 방법 알려줘."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - SH(서울주택도시공사) 청약 공고 조회 및 필터링 (Priority: P1) 🎯 MVP

사용자가 청약 일정 탭의 서브 메뉴 또는 뷰 전환 단추를 클릭해 SH(서울주택도시공사)의 분양 및 임대 공고 일정을 조회할 수 있어야 한다. 서울 지역 및 타겟 경기도 4개 지역에 부합하는 공고들을 상태(접수중/공고중)별로 필터링하여 보여준다.

**Why this priority**:
서울주택도시공사(SH)는 서울 지역 공공주택 공급의 주축이므로 청약 정보를 수집할 때 LH와 함께 가장 중요도가 높은 핵심 요구사항이다.

**Independent Test**:
`SH_API_KEY`가 정의된 로컬 환경에서 `curl http://localhost:8080/api/subscriptions/sh/today`를 호출하여 SH 공고 데이터가 누락 없이 성공적으로 파싱되어 반환되는지 독립 검증한다.

**Acceptance Scenarios**:
1. **Given** `.env` 파일에 유효한 `SH_API_KEY`가 설정되어 있음
   **When** 브라우저에서 'SH 공공' 탭을 선택함
   **Then** 접수중 및 공고중인 SH 분양/임대 공고들이 목록에 지도 핀 및 날짜와 함께 표시된다.

---

### User Story 2 - GH(경기주택도시공사) 청약 공고 조회 및 필터링 (Priority: P2)

사용자가 청약 일정 탭에서 GH(경기주택도시공사)의 분양 및 임대 공고 일정을 조회할 수 있어야 한다. 경기 4개 지역(의정부, 남양주, 하남, 구리) 및 서울 인접 구역 공고들을 상태(접수중/공고중)별로 필터링하여 보여준다.

**Why this priority**:
경기도 공공 청약의 큰 비중을 차지하므로, GH 공고 조회를 지원하여 완결성 높은 공공 청약 일정 대시보드를 구축한다.

**Independent Test**:
`GH_API_KEY`가 정의된 로컬 환경에서 `curl http://localhost:8080/api/subscriptions/gh/today`를 호출하여 GH 공고 데이터가 누락 없이 성공적으로 파싱되어 반환되는지 독립 검증한다.

**Acceptance Scenarios**:
1. **Given** `.env` 파일에 유효한 `GH_API_KEY`가 설정되어 있음
   **When** 브라우저에서 'GH 공공' 탭을 선택함
   **Then** 접수중 및 공고중인 GH 분양/임대 공고들이 목록에 표시된다.

---

### Edge Cases

- **공공 데이터 API 키 미신청 상태**: 사용자가 SH/GH API 키를 `.env`에 정의하지 않은 경우, 화면에 경고 알림(KeyMissingNotice) 카드를 출력하고 해당 영역의 API 호출을 우아하게 생략해야 한다.
- **지역명 필터링 노이즈**: SH/GH 공고 명칭에 기재된 지역 텍스트(예: "서울 마곡", "남양주 다산")를 파싱해 서울 및 경기 4개 타겟 지역 필터링이 누락 없이 정확하게 걸러지는지 검증해야 한다.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 백엔드는 공공데이터포털(data.go.kr)의 오픈 API를 통해 SH 및 GH 분양/임대 공고 일정을 조회하는 HTTP 클라이언트를 구현해야 한다.
- **FR-002**: 시스템은 `SH_API_KEY` 및 `GH_API_KEY` 환경 변수를 사용해 API 호출 인증을 처리해야 한다.
- **FR-003**: 수집된 SH/GH 공고 중 타겟 지역(서울 및 경기 4개 시군구)에 해당하는 공고만 필터링하여 정렬한다.
- **FR-004**: 시스템은 SH API 호출을 위해 [NEEDS CLARIFICATION: SH API 인증키 및 상세 호출 오퍼레이션 정보 요구]를 지원해야 한다.
- **FR-005**: 시스템은 GH API 호출을 위해 [NEEDS CLARIFICATION: GH API 인증키 및 상세 호출 오퍼레이션 정보 요구]를 지원해야 한다.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 청약 일정 탭의 상단 토글 메뉴에 'SH 공공' 및 'GH 공공'이 추가되어 탭 간 전환이 0.5초 이내에 완료된다.
- **SC-002**: API 미설정 시 노란색 경고 카드 형태로 가이드가 제공되며, API 키 설정 시 정상 데이터가 렌더링된다.

## Assumptions

- 공공데이터포털의 API 구조 및 응답 필드가 LH 공사 API의 형태(PAN_NM, PAN_ID 등)와 유사하거나 표준 공공데이터 JSON 규격을 따른다고 가정한다.
