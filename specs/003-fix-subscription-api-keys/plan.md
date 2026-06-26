# Implementation Plan: fix-subscription-api-keys

**Branch**: `003-fix-subscription-api-keys` | **Date**: 2026-06-25 | **Spec**: [spec.md](file:///home/lwt/dev/personal/account/specs/003-fix-subscription-api-keys/spec.md)

**Input**: Feature specification from `specs/003-fix-subscription-api-keys/spec.md`

## Summary

로컬 실행 환경에서 `.env` 파일에 정의된 API Key들(`APPLYHOME_API_KEY`, `LH_API_KEY`)이 백엔드 구동 프로세스에 주입되지 않아, 청약일정 조회 탭 접속 시 "API 키가 설정되지 않았습니다" 오류 카드가 발생하는 문제를 해결합니다. 
이를 위해 `backend/build.gradle` 파일 내의 `bootRun` 태스크에 `.env` 파일을 자동으로 파싱하고 JVM 환경 변수로 `export` 주입하는 Gradle DSL 스크립트 설정을 반영합니다.

## Technical Context

**Language/Version**: Java 17, TypeScript 5.9, React 19, Vite 7

**Primary Dependencies**: Spring Boot 3.4.1, Spring RestClient, Gradle

**Storage**: N/A (설정 기동 관련)

**Testing**: JUnit 5, Mockito (TDD 원칙에 따라, 주입 여부를 스프링 콘텍스트 통합 환경에서 검증하는 테스트 코드를 선행 작성합니다)

**Target Platform**: PC-optimized Web (Chrome/Safari)

**Project Type**: Web Application (Spring Boot Backend + React Frontend)

**Performance Goals**: N/A (설정 기동 관련)

**Constraints**: 모든 UI 명칭 및 주석, 커밋 메시지는 한국어 필수 사용. 본 프로젝트 헌법 v1.0.0의 핵심 가치를 준수함.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: TDD First**: 빌드 변경 로직 적용에 앞서, 백엔드 스프링 환경 로드 단위 테스트에 `.env` 로딩이 잘 되었는지를 입증하는 간이 테스트(예: `SubscriptionServiceTest` 내 Property 로드 검증 혹은 환경변수 확인)를 구성하여 로컬 구동 시 누락되는 케이스가 없도록 Red 상태를 검증합니다.
- **Principle II: Request Info**: 사용자의 로컬 오류 정황과 설정 값을 확보하여 명세 단계에 모두 반영 완료했습니다.
- **Principle III: Harness-Driven**: 기동 시점에 로딩 상태를 즉각 입증할 수 있도록 curl 호출 검증 가이드를 [quickstart.md](file:///home/lwt/dev/personal/account/specs/003-fix-subscription-api-keys/quickstart.md)에 하네스 시나리오로 구현했습니다.

## Project Structure

### Documentation (this feature)

```text
specs/003-fix-subscription-api-keys/
├── plan.md              # This file
├── research.md          # Investigation & Decisions
├── data-model.md        # Environment variables configuration mapping
└── quickstart.md        # Manual Verification Guide
```

### Source Code (repository root)

```text
backend/
└── build.gradle         # bootRun 환경변수 자동 로드 로직 추가 대상
```

**Structure Decision**: 기존 백엔드의 빌드 구성 관리 파일인 [build.gradle](file:///home/lwt/dev/personal/account/backend/build.gradle) 내의 `bootRun` 설정을 조작하여 `.env` 파싱 로직을 격리 구현하며, 다른 Java 클래스 및 서비스 계층은 수정 없이 빌드 타겟만 정밀 조정합니다.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 없음 | 최소한의 Gradle 구성 수정만으로 환경변수 주입을 투명화하여 복잡도가 매우 낮음 | - |
