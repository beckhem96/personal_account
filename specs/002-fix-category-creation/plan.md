# Implementation Plan: fix-category-creation

**Branch**: `002-fix-category-creation` | **Date**: 2026-06-25 | **Spec**: [spec.md](file:///home/lwt/dev/personal/account/specs/002-fix-category-creation/spec.md)

**Input**: Feature specification from `specs/002-fix-category-creation/spec.md`

## Summary

카테고리 생성 및 수정 시 연말정산 분류(`YearEndCategory`) 필드에 빈 문자열(`""`) 또는 `null`이 전달되는 상황에서도 Jackson 역직렬화 오류(`HttpMessageNotReadableException`)가 발생하지 않도록 역직렬화 메커니즘을 보완하고 비즈니스 예외 안전성을 확보합니다. 
또한 카테고리 이름의 필수값 검증(Trimming 공백 검증 및 중복 체크)을 프론트엔드와 백엔드 모두에 강제하고, 지출(`EXPENSE`) 외의 유형에서는 연말정산 매핑을 항상 `NONE`으로 고정하는 정책을 비즈니스 코드로 확실히 정의합니다.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.9, React 19, Vite 7

**Primary Dependencies**: Spring Boot 3.4.1, Spring Data JPA, Axios, Tailwind CSS 4, Jackson

**Storage**: MySQL 8.0 (Docker)

**Testing**: JUnit 5, Spring Boot Test, Vitest (TDD 적용)

**Target Platform**: PC-optimized Web (Chrome/Safari)

**Project Type**: Web Application (Spring Boot Backend + React Frontend)

**Performance Goals**: 카테고리 생성 및 수정 API 완료 시간 0.5초 이내

**Constraints**: 모든 금액은 `BigDecimal` 필수 사용, DTO는 Java `record`로 구현, 엔티티에 public setter 금지, UI 텍스트 및 주석은 모두 한국어로 통일

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: TDD First**: 모든 로직 추가/수정에 앞서 테스트 코드를 선제 작성한다.
  - 백엔드: `CategoryServiceTest.java`에 카테고리 이름 빈 값/중복 예외 발생 테스트, 수입/이체 시 `YearEndCategory.NONE` 고정 테스트, 그리고 빈 문자열(`""`) 역직렬화 검증용 JUnit/Jackson 역직렬화 단위 테스트를 선구현하여 Red(실패) 상태를 검증한 후 구현에 들어간다.
  - 프론트엔드: 이름 입력값 공백 트리밍 및 필수값 검증 기능 단위 테스트를 보강한다.
- **Principle II: Request Info**: 도메인 예외 사항(수입/이체 카테고리의 신용카드 소득공제 매핑 제한)에 대한 정책을 명세에 사전 반영 완료하여 설계에 차출한다.
- **Principle III: Harness-Driven**: Jackson 디시리얼라이저 바인딩 테스트 하네스(역직렬화 전용 간이 테스트 케이스)를 구축하여 JSON 페이로드 입력을 통한 검증 루프를 빠르게 수행한다.

## Project Structure

### Documentation (this feature)

```text
specs/002-fix-category-creation/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── checklists/
    └── requirements.md  # Quality Checklist
```

### Source Code (repository root)

```text
backend/
├── src/main/java/org/example/account/
│   ├── domain/               # Category.java, YearEndCategory.java
│   ├── dto/                  # CategoryRequest.java, CategoryResponse.java
│   └── service/              # CategoryService.java
└── src/test/java/org/example/account/
    └── service/              # CategoryServiceTest.java (TDD 선행 구현 대상)

frontend/
├── src/
│   ├── pages/
│   │   └── Settings.tsx      # 프론트엔드 입력 트림 검증 및 폼 기본값 보정
│   └── types/
│       └── index.ts
```

**Structure Decision**: 기존 Spring Boot 및 React 프로젝트 구조 내의 카테고리 관리 컴포넌트([Settings.tsx](file:///home/lwt/dev/personal/account/frontend/src/pages/Settings.tsx)), 백엔드 DTO 및 서비스 구조([CategoryService.java](file:///home/lwt/dev/personal/account/backend/src/main/java/org/example/account/service/CategoryService.java))를 타겟으로 수정하며, 비즈니스 핵심 코드를 수정하기 전에 JUnit 테스트 클래스를 새로 작성하여 견고성을 다집니다.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 없음 | 모든 구현 계획이 헌법과 컨벤션 원칙을 철저하게 준수함 | - |
