<!--
Sync Impact Report:
- Version change: [TEMPLATE] -> 1.0.0
- List of modified principles:
  - Added Principle I: 모든 기능은 수정, 추가, 보완 시 TDD 필수 진행 (TDD First)
  - Added Principle II: 필요한 정보는 사용자에게 요구 (Request Required Info)
  - Added Principle III: Andrej Karpathy의 테스트 하네스 기법 적용 (Harness-Driven Development)
- Added sections:
  - 기술적 제약 사항 및 코딩 컨벤션 (Technical Constraints & Conventions)
  - 개발 워크플로우 및 검증 절차 (Development Workflow & Verification)
- Templates requiring updates:
  - .specify/templates/tasks-template.md (✅ updated)
- Follow-up TODOs: None
-->

# Personal Finance Dashboard Constitution

## Core Principles

### I. 모든 기능은 수정, 추가, 보완 시 TDD 필수 진행 (TDD First)
모든 새로운 기능의 추가, 기존 기능의 수정 및 보완 작업 시 테스트 코드를 먼저 작성하여 실패하는 것을 확인한 후(Red), 이를 통과하도록 코드를 구현하고(Green), 이후 리팩토링하는 Red-Green-Refactor 사이클을 엄격히 준수한다.
- **이유**: 구현 전에 올바른 스펙과 엣지 케이스를 테스트로 강제함으로써 설계 결함을 사전에 방지하고 코드의 신뢰성을 극대화한다.

### II. 모호하거나 필요한 정보는 사용자에게 요구 (Request Required Info)
요구사항이나 기능 설계가 모호한 영역, 혹은 추가적인 비즈니스 규칙이나 도메인 정보가 필요한 경우, 자의적으로 판단하여 구현하지 않고 사용자에게 적극적으로 질문하여 명확한 요구사항을 획득한 뒤 작업에 착수한다.
- **이유**: 잘못된 가정 하에 개발하는 리스크를 최소화하고, 사용자의 실제 비즈니스 요건에 완벽히 부합하는 기능 개발을 보장한다.

### III. Andrej Karpathy의 테스트 하네스 기법 적용 (Harness-Driven Development)
기능 개발이나 디버깅을 시작하기 전에 실행 흐름을 자동으로 주입하고 실시간으로 검증할 수 있는 단위 테스트 또는 독립적인 실행 하네스(Harness) 환경을 먼저 구축한다. 개발 시간의 상당 부분을 하네스 및 자동화된 테스트 환경 설계에 투자하여 피드백 주기를 극대화한다.
- **이유**: 반복적인 개발 피드백 속도를 최적화하고 에이전트의 안정적인 자율 개발을 뒷받침하기 위해 견고한 자동화 검증 프레임워크가 선행되어야 한다.

## 기술적 제약 사항 및 코딩 컨벤션 (Technical Constraints & Conventions)

- **백엔드(Spring Boot)**: 모든 DTO는 Java `record` 타입을 사용해야 하며, 금액은 절대 `double`/`float` 대신 `BigDecimal`을 사용해야 한다. 엔티티 상태 변경은 public setter 대신 도메인 내부의 명확한 비즈니스 메서드로만 수행한다. 없는 리소스 접근 시 `IllegalArgumentException`을 발생시킨다.
- **프론트엔드(React/TypeScript)**: 전역 상태 관리 라이브러리를 사용하지 않고 로컬 state만을 사용하여 간단하고 직관적인 상태 관리를 유지한다. 스타일링은 오직 Tailwind CSS 유틸리티 클래스만 적용하며, 모든 TypeScript 타입은 `types/index.ts`에서 중앙 집중하여 관리한다.

## 개발 워크플로우 및 검증 절차 (Development Workflow & Verification)

- **검증 절차**: 코드 변경 작업 완료 후 백엔드는 `./gradlew build`로 컴파일 및 테스트 통과를 보장해야 하며, 프론트엔드는 `npm run build` 및 `npm run lint` 검사를 에러 없이 완료해야 한다.
- **언어 규칙**: UI 텍스트, 커밋 메시지, 도메인 로직 관련 주석 및 설계 문서는 오직 **한국어**만을 사용하여 명확성을 확보한다.

## Governance

- 본 헌법은 프로젝트의 최상위 의사결정 지침으로, 어떠한 임의적 리팩토링이나 기능 추가도 헌법에 작성된 원칙을 위배할 수 없다.
- 헌법의 추가/수정은 문서 업데이트와 버전 증가를 필수로 동반하며, 관련된 템플릿 파일들의 일관성 있는 업데이트 및 영향 보고가 수반되어야 한다.

**Version**: 1.0.0 | **Ratified**: 2026-06-25 | **Last Amended**: 2026-06-25
