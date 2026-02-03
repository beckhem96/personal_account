# CLAUDE.md - AI 어시스턴트를 위한 프로젝트 가이드

## 프로젝트 개요

PC 환경에 최적화된 **개인 자산 관리 및 세무 보조 웹 애플리케이션**이다.
사용자는 예산 수립, 지출/수입 기록(가계부), 자산 현황 파악, 한국 세법 기반 세금 계산 및 연말정산 시뮬레이션을 하나의 대시보드에서 수행할 수 있다.

**도메인 언어는 한국어이다.** 커밋 메시지, UI 텍스트, 도메인 로직 관련 주석, 문서는 한국어로 작성한다. 이 관례를 유지할 것.

## 기술 스택

| 계층 | 기술 |
|------|------|
| 백엔드 | Java 17+, Spring Boot 3.4.1, Gradle |
| 데이터베이스 | MySQL 8.0 (Docker), Spring Data JPA (Hibernate) |
| 프론트엔드 | React 19, TypeScript 5.9, Vite 7 |
| 스타일링 | Tailwind CSS 4 |
| HTTP 클라이언트 | Axios |
| 차트 | Recharts |
| 아이콘 | Lucide React |

## 빠른 시작

```bash
# 1. 데이터베이스 실행
docker-compose up -d

# 2. 백엔드 실행 (포트 8080)
cd backend && ./gradlew bootRun

# 3. 프론트엔드 실행 (포트 5173, /api를 :8080으로 프록시)
cd frontend && npm install && npm run dev
```

## 프로젝트 구조

```
personal_account/
├── backend/src/main/java/org/example/account/
│   ├── controller/        # 7개 REST 컨트롤러 (@RestController)
│   ├── service/           # 7개 서비스 클래스 (@Service)
│   ├── repository/        # 6개 JPA 리포지토리 (JpaRepository 인터페이스)
│   ├── domain/            # 9개 JPA 엔티티 + Enum
│   ├── dto/               # Request/Response DTO (Java record)
│   └── config/            # DataInitializer (기본 카테고리 시드 데이터), SchedulingConfig (@EnableScheduling)
├── frontend/src/
│   ├── pages/             # Dashboard.tsx, Budget.tsx, Assets.tsx, Tax.tsx
│   ├── components/        # Layout.tsx (사이드바 + 네비게이션)
│   ├── api/               # client.ts (Axios 인스턴스), services.ts (API 호출)
│   ├── types/             # index.ts (전체 TypeScript 인터페이스)
│   └── utils/             # formatCurrency, cn (clsx + tailwind-merge)
├── docker-compose.yml     # MySQL 8.0 컨테이너 설정
└── settings.gradle        # Gradle 멀티프로젝트 (includes 'backend')
```

## 백엔드 컨벤션

### 엔티티 패턴
- Lombok: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- ID 전략: `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- Enum: `@Enumerated(EnumType.STRING)`
- 연관관계: `@ManyToOne(fetch = FetchType.LAZY)` (항상 지연 로딩)
- 상태 변경은 도메인 메서드(`update(...)`, `confirm()` 등)로 처리 — public setter 금지

### DTO 패턴
- 모든 DTO는 Java `record` 타입으로 작성
- 엔티티 → Response 변환은 정적 팩토리 메서드 `from(Entity entity)` 사용
- Request와 Response는 별도 record로 분리

### 서비스 패턴
- `@Service` + `@RequiredArgsConstructor` (Lombok을 통한 생성자 주입)
- 조회 메서드: `@Transactional(readOnly = true)`
- 변경 메서드: `@Transactional`
- 존재하지 않는 리소스: `IllegalArgumentException` 발생
- 컬렉션 변환은 Stream API 사용

### 컨트롤러 패턴
- `@RestController` + `@RequestMapping("/api/...")`
- `@RequiredArgsConstructor`로 의존성 주입
- 모든 엔드포인트는 `ResponseEntity<T>` 반환
- LocalDate 파라미터: `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` 사용

### 금액 처리
- **금액은 반드시 `BigDecimal`** 사용. `double`이나 `float` 절대 사용 금지.

## 프론트엔드 컨벤션

### 컴포넌트 패턴
- 함수형 컴포넌트 + 훅(useState, useEffect) 사용
- 상태 관리 라이브러리 미사용 — 로컬 state만 사용
- 페이지별 useEffect에서 데이터 fetch
- 에러 처리: `console.error` + 사용자에게 보이는 에러 상태 표시

### 스타일링
- Tailwind CSS 유틸리티 클래스만 사용
- 조건부 클래스 적용 시 `cn()` 유틸리티 (`utils/`에 위치, clsx + tailwind-merge)
- 일관된 색상 팔레트: slate, blue, green, red, purple
- 반응형: `md:` 브레이크포인트 사용 (모바일 우선)

### 타입 안전성
- 모든 타입은 `types/index.ts`에 중앙 관리
- 백엔드 DTO와 매칭되는 Request/Response 타입 쌍
- Enum은 유니온 타입: `type PaymentMethod = 'CASH' | 'CARD' | 'BANK_TRANSFER'`

### API 계층
- Axios 인스턴스: `baseURL: '/api'` (개발 환경에서 백엔드로 프록시)
- `api/services.ts`에 서비스 함수 — 모든 함수 async, 타입 반환
- 페이지 로딩 시 `Promise.all`로 병렬 요청

### 라우팅
- React Router v7 + BrowserRouter
- 경로: `/` (대시보드), `/budget`, `/assets`, `/tax`
- 모든 페이지는 `Layout` 컴포넌트로 감쌈

## 데이터 모델

### 핵심 엔티티
- **Category (카테고리)** — `INCOME | EXPENSE | TRANSFER`. 앱 시작 시 10개 기본값 자동 생성.
- **Transaction (거래 내역)** — date, amount, memo, paymentMethod, category(FK), card(FK, nullable), recurringTransaction(FK, nullable), isConfirmed(예정/확정 구분)
- **Budget (예산)** — year, month, amount, category(FK). (year, month, category) 유니크 제약.
- **Asset (자산)** — type(`CASH | SAVINGS | STOCK | DEBT`), name, balance, purchasePrice(nullable, 주식용)
- **Card (카드)** — name, type(`CREDIT | CHECK`)
- **RecurringTransaction (고정 비용)** — name, amount, dayOfMonth, paymentMethod, card(FK, nullable), category(FK). 생성 시 해당 월의 Transaction 자동 생성. 매월 1일 스케줄러 및 앱 시작 시 자동 생성.

### Enum 목록
- `TransactionType`: INCOME, EXPENSE, TRANSFER
- `PaymentMethod`: CASH, CARD, BANK_TRANSFER
- `CardType`: CREDIT, CHECK
- `AssetType`: CASH, SAVINGS, STOCK, DEBT

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/transactions?startDate=&endDate=` | 기간별 거래 조회 |
| GET | `/api/transactions/planned` | 미확정(예정) 거래 조회 |
| GET | `/api/transactions/by-card/{cardId}` | 카드별 거래 조회 (날짜 파라미터 선택) |
| POST | `/api/transactions` | 거래 생성 |
| PUT | `/api/transactions/{id}` | 거래 수정 |
| DELETE | `/api/transactions/{id}` | 거래 삭제 |
| PATCH | `/api/transactions/{id}/confirm` | 거래 확정 처리 |
| GET | `/api/budgets?year=&month=` | 월별 예산 조회 |
| POST | `/api/budgets` | 예산 생성/수정 (upsert) |
| GET | `/api/assets` | 전체 자산 조회 |
| GET | `/api/assets/net-worth` | 순자산 계산 (자산/부채/순자산 분류) |
| POST | `/api/assets` | 자산 생성 |
| PUT | `/api/assets/{id}` | 자산 수정 |
| GET | `/api/categories` | 전체 카테고리 조회 |
| POST | `/api/categories` | 카테고리 생성 |
| GET | `/api/cards` | 전체 카드 조회 |
| POST | `/api/cards` | 카드 생성 |
| DELETE | `/api/cards/{id}` | 카드 삭제 |
| GET | `/api/recurring` | 전체 고정 비용 조회 |
| POST | `/api/recurring` | 고정 비용 생성 (해당 월 소비에 자동 추가) |
| DELETE | `/api/recurring/{id}` | 고정 비용 삭제 |
| POST | `/api/tax/stock` | 주식 양도소득세 계산 |
| POST | `/api/tax/year-end` | 연말정산 시뮬레이션 |

## 도메인 로직 (한국 세법)

### 주식 양도소득세
- 공식: `(매도금액 - 매수금액 - 기본공제 250만원) × 22%`
- 22% = 소득세 20% + 지방소득세 2%
- 차익이 250만원 이하이면 세금 없음

### 연말정산 (소득공제)
- 최소 사용 기준: 총급여의 25%
- 신용카드 공제율: 15%
- 체크카드/현금영수증 공제율: 30%
- 예상 공제액 계산 및 남은 기간 소비 가이드 제공

## 빌드 및 검증 명령어

```bash
# 백엔드
cd backend
./gradlew build          # 컴파일 + 테스트
./gradlew test           # 테스트만 실행
./gradlew bootRun        # 서버 실행

# 프론트엔드
cd frontend
npm run build            # TypeScript 검사 + Vite 빌드
npm run lint             # ESLint 검사
npm run dev              # 개발 서버 (HMR 지원)
```

## 테스트

- 백엔드: JUnit 5 (`spring-boot-starter-test`). 현재는 컨텍스트 로드 테스트만 존재.
- 프론트엔드: 테스트 프레임워크 미설정.
- 기능 추가 시 최소한 서비스 레이어의 비즈니스 로직 테스트를 작성할 것.

## 설정 파일

- `backend/src/main/resources/application.yml` — DB 연결, JPA 설정, 타임존(Asia/Seoul)
- `docker-compose.yml` — MySQL 8.0 컨테이너 (포트 3306, DB명: `account`, 문자셋: utf8mb4)
- `frontend/vite.config.ts` — 개발 프록시 `/api` → `localhost:8080`
- `frontend/tsconfig.json` — TypeScript strict 모드 설정
- `frontend/tailwind.config.js` — Tailwind 콘텐츠 경로 설정

## 로드맵 (미구현)

- [x] 고정 비용 월별 자동 생성 (Spring Scheduler 활용)
- [ ] 실제 거래 데이터 기반 연말정산 자동 연동
- [ ] 거래 생성/확정 시 자산 잔액 자동 업데이트
- [ ] 성능 최적화 및 분석 기능
