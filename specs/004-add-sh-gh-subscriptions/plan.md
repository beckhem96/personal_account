# Implementation Plan: add-sh-gh-subscriptions

**Branch**: `004-add-sh-gh-subscriptions` | **Date**: 2026-06-26 | **Spec**: [spec.md](file:///home/lwt/dev/personal/account/specs/004-add-sh-gh-subscriptions/spec.md)

**Input**: Feature specification from `specs/004-add-sh-gh-subscriptions/spec.md`

## Summary

SH(서울주택도시공사) 및 GH(경기주택도시공사)의 분양·임대 청약 공고 데이터를 통합 수집하여 청약 일정 대시보드에 표출합니다. 
이를 위해 국토교통부 마이홈포털 오픈 API를 호출하는 `MyHomeClient`를 백엔드에 신규 구축하고, 공급기관명(`suplyInsttNm`) 필드를 기준으로 SH 및 GH 데이터를 필터링·가공하여 반환하는 비즈니스 레이어를 추가합니다. 
프론트엔드에는 SH 및 GH 전용 탭 토글과 카드 레이아웃을 탑재합니다.

## Technical Context

**Language/Version**: Java 17, TypeScript 5.9, React 19, Vite 7

**Primary Dependencies**: Spring Boot 3.4.1, Spring RestClient, jackson-dataformat-xml (XML 파싱용)

**Storage**: N/A (연동 API 조회 성격)

**Testing**: JUnit 5, Mockito (TDD 원칙 준수)

**Target Platform**: PC-optimized Web (Chrome/Safari)

**Project Type**: Web Application (Spring Boot Backend + React Frontend)

**Performance Goals**: API 호출 및 캐싱 적용 고려 (응답 지연 최소화)

**Constraints**: 모든 UI 명칭 및 주석, 커밋 메시지는 한국어 필수 사용.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: TDD First**: 마이홈 API 파싱 결과가 SH와 GH로 올바르게 분기 가공되는지에 대해 `MyHomeSubscriptionServiceTest`를 선제 작성하여 Red 상태를 확인하고, 비즈니스 코드를 올바르게 구현합니다.
- **Principle II: Request Info**: 포털 시뮬레이터를 기반으로 입증된 API 요건 명세를 계획에 전부 차출했습니다.
- **Principle III: Harness-Driven**: 기동 시점 데이터를 가시적으로 확인하기 위한 수동 검증 하네스를 [quickstart.md](file:///home/lwt/dev/personal/account/specs/004-add-sh-gh-subscriptions/quickstart.md)에 정비 완료했습니다.

## Project Structure

### Documentation (this feature)

```text
specs/004-add-sh-gh-subscriptions/
├── plan.md              # This file
├── research.md          # Investigation and decisions
├── data-model.md        # API field and property mapping
└── quickstart.md        # Manual verification scenarios
```

### Source Code (repository root)

```text
backend/
├── src/main/java/org/example/account/
│   ├── client/
│   │   └── MyHomeClient.java            # 마이홈 API HTTP 통신 담당
│   ├── config/
│   │   └── RestClientConfig.java        # RestClient 빈 추가 설정
│   ├── controller/
│   │   └── SubscriptionController.java  # SH/GH 엔드포인트 추가
│   └── service/
│       └── MyHomeSubscriptionService.java # SH/GH 비즈니스 필터링
└── src/test/java/org/example/account/
    └── service/
        └── MyHomeSubscriptionServiceTest.java # TDD 단위 테스트

frontend/
├── src/
│   ├── api/
│   │   └── services.ts                  # SH/GH API 호출 함수 추가
│   ├── pages/
│   │   └── Subscriptions.tsx            # SH/GH 탭 UI 렌더링 확장
│   └── types/
│       └── index.ts                     # SH/GH 응답 타입 매핑
```

**Structure Decision**: 기존 청약 일정 서비스 구조([SubscriptionController.java](file:///home/lwt/dev/personal/account/backend/src/main/java/org/example/account/controller/SubscriptionController.java))와 병렬적으로 마이홈 포털 연동 비즈니스 컴포넌트를 정교하게 신설 주입합니다.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 없음 | 기존 LH/청약홈 클라이언트 설계 패턴과 완전히 동일하게 구현하여 복잡성 증가가 없음 | - |
