# AGENTS.md - AI 에이전트 행동 가이드

이 파일은 AI 코딩 에이전트(Claude, Codex, Cursor 등)가 즉시 행동할 수 있도록 명령어와 규칙을 정리한 체크리스트이다. 도메인/구조 설명은 `CLAUDE.md` 참조.

## 빠른 시작

```bash
./start.sh          # DB + 백엔드 + 프론트엔드 한 번에 실행
# Ctrl+C로 종료 (DB는 유지됨)
docker-compose down # DB 완전 종료
```

## 빌드 및 검증 명령어

| 영역 | 명령어 | 설명 |
|------|--------|------|
| 백엔드 컴파일 | `cd backend && ./gradlew build` | 컴파일 + 테스트 |
| 백엔드 테스트 | `cd backend && ./gradlew test` | 테스트만 |
| 프론트 빌드 | `cd frontend && npm run build` | TypeScript + Vite 빌드 |
| 프론트 린트 | `cd frontend && npm run lint` | ESLint 검사 |

**변경 후 반드시 해당 영역 빌드 확인할 것.**

## 코딩 컨벤션 요약

### 백엔드 (Java/Spring)
- DTO는 `record` 타입
- 금액은 반드시 `BigDecimal` (double/float 금지)
- 엔티티 상태 변경은 도메인 메서드로 (public setter 금지)
- 조회 메서드 `@Transactional(readOnly = true)`, 변경 메서드 `@Transactional`
- 없는 리소스 접근 시 `IllegalArgumentException` 발생

### 프론트엔드 (React/TypeScript)
- 함수형 컴포넌트 + `useState`/`useEffect`
- 상태 관리 라이브러리 없음 (로컬 state만)
- 스타일링은 Tailwind CSS 유틸리티 클래스만
- 조건부 클래스는 `cn()` 유틸리티 사용 (`utils/` 참조)
- 모든 타입은 `types/index.ts`에 중앙 관리

## 금지 사항

- 한국어 도메인 표현/UI 텍스트를 영어로 바꾸지 말 것
- 금액에 `double`/`float` 사용 금지
- 엔티티에 public setter 추가 금지
- 요구사항 외 기능 추가/리팩토링 금지
- 불필요한 추상화/오버엔지니어링 금지

## 커밋 및 PR

- **커밋 메시지**: 한국어로 작성
- **브랜치**: 기능별 브랜치 사용 권장
- **PR 본문**: 변경 사항 요약 + 테스트 방법

## 주요 파일 위치

| 용도 | 경로 |
|------|------|
| 백엔드 엔티티 | `backend/src/main/java/org/example/account/domain/` |
| 백엔드 서비스 | `backend/src/main/java/org/example/account/service/` |
| 백엔드 DTO | `backend/src/main/java/org/example/account/dto/` |
| 프론트 페이지 | `frontend/src/pages/` |
| 프론트 타입 | `frontend/src/types/index.ts` |
| API 서비스 | `frontend/src/api/services.ts` |
| DB 설정 | `docker-compose.yml` |
| 실행 스크립트 | `start.sh` |

## 변경 후 체크리스트

- [ ] 백엔드 변경 → `./gradlew build` 통과
- [ ] 프론트 변경 → `npm run build` 통과
- [ ] 타입 변경 → 백엔드 DTO와 프론트 타입 동기화 확인
- [ ] API 변경 → `api/services.ts` 함수 업데이트
- [ ] 커밋 메시지 한국어로 작성

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
at specs/001-cashflow-ledger-dashboard/plan.md
<!-- SPECKIT END -->
