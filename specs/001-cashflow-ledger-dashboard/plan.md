# Implementation Plan: Simplified Cashflow Ledger & Budget Dashboard

**Branch**: `001-cashflow-ledger-dashboard` | **Date**: 2026-06-25 | **Spec**: [Simplified Cashflow Ledger & Budget Dashboard Specification](file:///home/lwt/dev/personal/account/specs/001-cashflow-ledger-dashboard/spec.md)

## Summary

사용자가 복잡하게 느끼던 가계부 화면을 예산 설정(`/budget`)과 거래 내역 관리(`/ledger`)로 명확하게 분리하여 사용성을 극대화합니다.
또한, 이번 달의 통합 현금흐름(소득, 지출, 이체, 저축/투자, 보험) 및 예산 대비 지출 비율을 경고음/경고색(진척도 바)으로 한눈에 파악할 수 있는 대시보드(`/`)를 구현합니다.
엑셀 파일 업로드를 통한 대량 거래 일괄 등록 시, 기존 하나카드 외에 신한카드 및 KB국민카드 명세서 엑셀을 추가 분석할 수 있는 파서를 적용하고, Natural Key 기반의 중복 거래 유니크 검증을 적용하여 가계부 등록 과정을 자동화합니다.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.9, React 19, Vite 7

**Primary Dependencies**: Spring Boot 3.4.1, Spring Data JPA, Apache POI 5.2.x, Recharts, Tailwind CSS 4, Lucide React, Axios

**Storage**: MySQL 8.0 (Docker)

**Testing**: JUnit 5, Spring Boot Test, Mockito (TDD 원칙에 따라 테스트 우선 작성)

**Target Platform**: PC-optimized Web (Chrome/Safari)

**Project Type**: Web Application (Spring Boot Backend + React Frontend)

**Performance Goals**: 대시보드 및 가계부 목록 로딩 시간 1초 이내, 엑셀 파싱 5초 이내 완료

**Constraints**: 모든 금액은 `BigDecimal` 필수 사용, DTO는 Java `record`로 구현, 엔티티에 public setter 금지

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: TDD First**: 모든 로직 추가 전 테스트 코드를 선제 작성한다.
  - 신한/국민카드 파서의 단독 유닛 테스트 코드(`ShinhanCardStatementParserTest`, `KBCardStatementParserTest`)를 작성하여 실패하는 것을 확인 후 구현에 들어간다.
  - `StatementImportService` 및 `TransactionService` 비즈니스 규칙(자산이동 출금/입금 필수 검증 등) 검증용 Mockito/Integration 테스트 케이스를 우선 작성한다.
- **Principle II: Request Info**: 개인 카드사 API 연동은 연동 기술적 인프라 한계로 배제하고 엑셀 명세서 업로드 및 수동 입력으로 범위를 제한하여 스펙에 반영 완료하였다.
- **Principle III: Harness-Driven**: 엑셀 파서 로직 검증 및 엑셀 데이터 주입을 위한 모의 엑셀 데이터 생성용 테스트 하네스(테스트용 헬퍼 클래스)를 구현하여 TDD의 원활한 피드백 루프를 지원한다.

## Project Structure

### Documentation (this feature)

```text
specs/001-cashflow-ledger-dashboard/
├── plan.md              # This file
├── research.md          # Research Notes
├── data-model.md        # Entity definitions & Schema updates
├── quickstart.md        # Feature setup & local verification instructions
└── contracts/
    └── api-contracts.md # REST API Specification
```

### Source Code (repository root)

```text
backend/
├── src/main/java/org/example/account/
│   ├── domain/               # CardCompany.java
│   ├── statement/            # ShinhanCardStatementParser.java, KBCardStatementParser.java
│   ├── service/              # StatementImportService.java, TransactionService.java
│   └── controller/           # TransactionController.java, StatementController.java
└── src/test/java/org/example/account/
    ├── statement/            # ShinhanCardStatementParserTest.java, KBCardStatementParserTest.java
    └── service/              # TransactionServiceTest.java, StatementImportServiceTest.java

frontend/
├── src/
│   ├── components/
│   │   └── Layout.tsx        # 네비게이션 메뉴 분리 및 한글화
│   ├── pages/
│   │   ├── Dashboard.tsx    # 현금흐름 요약 카드 & Recharts & 예산 대비 진척도
│   │   ├── Budget.tsx       # 예산 설정 전용 페이지 (거래 내역 제거)
│   │   ├── Ledger.tsx       # 신규 거래 관리 페이지 (필터, 목록, 수동 입력 모달)
│   │   └── Statements.tsx   # 신한/국민카드 카드사 드롭다운 노출 확인
│   ├── App.tsx              # /ledger 라우트 등록
│   └── types/
│       └── index.ts         # 타입 매핑 확인
```

**Structure Decision**: 기존 Spring Boot 백엔드와 React 프론트엔드가 명확히 분리되어 있는 구조를 유지하며, `Budget.tsx` 파일 내에서 관리하던 거래 목록 및 폼 로직을 완전히 신규 컴포넌트인 `Ledger.tsx`로 이관하여 도메인 복잡성을 낮춥니다.
또한, 엑셀 파서의 경우 기존 `CardStatementParser` 인터페이스의 구현체로서 클래스를 추가하여 `CardStatementParserRegistry`를 통한 빈 자동 주입으로 확장하는 구조적 정합성을 갖춥니다.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 없음 | 헌법에 작성된 규칙 및 프로젝트 가이드라인을 완전하게 따름 | - |
