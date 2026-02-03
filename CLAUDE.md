# CLAUDE.md - AI Assistant Guide for Personal Finance Dashboard

## Project Summary

Korean personal finance management web application (PC-optimized). Users manage budgets, record transactions, track assets, and simulate Korean tax calculations from a single dashboard.

**Domain language is Korean.** Commit messages, UI text, comments in domain logic, and documentation are often in Korean. Maintain this convention.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17+, Spring Boot 3.4.1, Gradle |
| Database | MySQL 8.0 (Docker), Spring Data JPA (Hibernate) |
| Frontend | React 19, TypeScript 5.9, Vite 7 |
| Styling | Tailwind CSS 4 |
| HTTP Client | Axios |
| Charts | Recharts |
| Icons | Lucide React |

## Quick Start

```bash
# 1. Database
docker-compose up -d

# 2. Backend (port 8080)
cd backend && ./gradlew bootRun

# 3. Frontend (port 5173, proxies /api to :8080)
cd frontend && npm install && npm run dev
```

## Project Structure

```
personal_account/
├── backend/src/main/java/org/example/account/
│   ├── controller/        # 7 REST controllers (@RestController)
│   ├── service/           # 7 service classes (@Service)
│   ├── repository/        # 6 JPA repositories (JpaRepository interfaces)
│   ├── domain/            # 9 JPA entities + enums
│   ├── dto/               # Request/Response DTOs (Java records)
│   └── config/            # DataInitializer (seeds default categories)
├── frontend/src/
│   ├── pages/             # Dashboard.tsx, Budget.tsx, Assets.tsx, Tax.tsx
│   ├── components/        # Layout.tsx (sidebar + navigation)
│   ├── api/               # client.ts (Axios instance), services.ts (API calls)
│   ├── types/             # index.ts (all TypeScript interfaces)
│   └── utils/             # formatCurrency, cn (clsx + tailwind-merge)
├── docker-compose.yml     # MySQL 8.0 container config
└── settings.gradle        # Gradle multi-project (includes 'backend')
```

## Backend Conventions

### Entity Pattern
- Lombok: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- ID strategy: `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- Enums: `@Enumerated(EnumType.STRING)`
- Relationships: `@ManyToOne(fetch = FetchType.LAZY)` (always lazy)
- Domain methods for mutations (e.g., `update(...)`, `confirm()`) — no public setters

### DTO Pattern
- Use Java `record` types for all DTOs
- Static factory method `from(Entity entity)` for entity-to-response conversion
- Separate Request and Response records

### Service Pattern
- `@Service` + `@RequiredArgsConstructor` (constructor injection via Lombok)
- `@Transactional(readOnly = true)` for read operations
- `@Transactional` for write operations
- Throw `IllegalArgumentException` for not-found errors
- Use Stream API for collection transformations

### Controller Pattern
- `@RestController` + `@RequestMapping("/api/...")`
- `@RequiredArgsConstructor` for injection
- Return `ResponseEntity<T>` from all endpoints
- Use `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` for LocalDate parameters

### Money
- **Always use `BigDecimal`** for monetary amounts. Never use `double` or `float`.

## Frontend Conventions

### Component Patterns
- Functional components with hooks (useState, useEffect)
- No state management library — local state only
- Pages fetch their own data in useEffect
- Error handling: `console.error` + user-visible error states

### Styling
- Tailwind CSS utility classes exclusively
- `cn()` utility (from `utils/`) for conditional classes (clsx + tailwind-merge)
- Consistent color palette: slate, blue, green, red, purple
- Responsive with `md:` breakpoints (mobile-first)

### Type Safety
- All types centralized in `types/index.ts`
- Request/Response type pairs matching backend DTOs
- Union types for enums: `type PaymentMethod = 'CASH' | 'CARD' | 'BANK_TRANSFER'`

### API Layer
- Axios instance with `baseURL: '/api'` (proxied to backend in dev)
- Service functions in `api/services.ts` — all async, typed returns
- `Promise.all` for parallel requests on page load

### Routing
- React Router v7 with BrowserRouter
- Routes: `/` (Dashboard), `/budget`, `/assets`, `/tax`
- All pages wrapped in single `Layout` component

## Data Model

### Core Entities
- **Category** — `INCOME | EXPENSE | TRANSFER`. Pre-seeded with 10 defaults on startup.
- **Transaction** — date, amount, memo, paymentMethod, category (FK), card (FK, nullable), isConfirmed (planned vs actual)
- **Budget** — year, month, amount, category (FK). Unique on (year, month, category).
- **Asset** — type (`CASH | SAVINGS | STOCK | DEBT`), name, balance, purchasePrice (nullable, for stocks)
- **Card** — name, type (`CREDIT | CHECK`)
- **RecurringTransaction** — name, amount, dayOfMonth, paymentMethod, card (FK, nullable), category (FK). Auto-creates a Transaction for the current month on creation.

### Enums
- `TransactionType`: INCOME, EXPENSE, TRANSFER
- `PaymentMethod`: CASH, CARD, BANK_TRANSFER
- `CardType`: CREDIT, CHECK
- `AssetType`: CASH, SAVINGS, STOCK, DEBT

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/transactions?startDate=&endDate=` | Query by date range |
| GET | `/api/transactions/planned` | Unconfirmed (future) transactions |
| GET | `/api/transactions/by-card/{cardId}` | Filter by card (optional date params) |
| POST | `/api/transactions` | Create transaction |
| PUT | `/api/transactions/{id}` | Update transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |
| PATCH | `/api/transactions/{id}/confirm` | Mark as confirmed |
| GET | `/api/budgets?year=&month=` | Monthly budgets |
| POST | `/api/budgets` | Create/upsert budget |
| GET | `/api/assets` | All assets |
| GET | `/api/assets/net-worth` | Net worth breakdown |
| POST | `/api/assets` | Create asset |
| PUT | `/api/assets/{id}` | Update asset |
| GET | `/api/categories` | All categories |
| POST | `/api/categories` | Create category |
| GET | `/api/cards` | All cards |
| POST | `/api/cards` | Create card |
| DELETE | `/api/cards/{id}` | Delete card |
| GET | `/api/recurring` | All recurring transactions |
| POST | `/api/recurring` | Create (auto-adds to current month) |
| DELETE | `/api/recurring/{id}` | Delete recurring transaction |
| POST | `/api/tax/stock` | Stock capital gains tax calculation |
| POST | `/api/tax/year-end` | Year-end tax settlement simulation |

## Domain Logic (Korean Tax)

### Stock Capital Gains Tax
- Formula: `(sellAmount - buyAmount - 2,500,000 KRW deduction) * 22%`
- 22% = 20% national + 2% local tax
- If profit <= 2.5M KRW, no tax owed

### Year-End Tax Settlement (연말정산)
- Minimum usage threshold: 25% of total salary
- Credit card deduction rate: 15%
- Debit card / cash receipt deduction rate: 30%
- Calculates estimated deduction and provides spending guidance

## Build & Verification Commands

```bash
# Backend
cd backend
./gradlew build          # Compile + test
./gradlew test           # Run tests only
./gradlew bootRun        # Start server

# Frontend
cd frontend
npm run build            # TypeScript check + Vite build
npm run lint             # ESLint
npm run dev              # Dev server with HMR
```

## Testing

- Backend: JUnit 5 via `spring-boot-starter-test`. Currently minimal (context load test only).
- Frontend: No test framework configured yet.
- When adding features, write service-layer tests for business logic at minimum.

## Configuration Files

- `backend/src/main/resources/application.yml` — DB connection, JPA settings, timezone (Asia/Seoul)
- `docker-compose.yml` — MySQL 8.0 container (port 3306, db: `account`, charset: utf8mb4)
- `frontend/vite.config.ts` — Dev proxy `/api` -> `localhost:8080`
- `frontend/tsconfig.json` — TypeScript strict mode config
- `frontend/tailwind.config.js` — Content paths for Tailwind purging

## Roadmap (Not Yet Implemented)

- [ ] Monthly recurring transaction auto-generation via Spring Scheduler
- [ ] Year-end tax settlement auto-populated from actual transaction data
- [ ] Asset balance auto-update when transactions are created/confirmed
- [ ] Performance optimizations and analytics features
