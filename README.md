# 개인 자산 관리 대시보드 (Personal Finance Dashboard)

PC 환경에 최적화된 **개인 자산 관리 및 세무 보조 웹 애플리케이션**입니다.
예산 관리, 지출/수입 기록, 자산 현황 파악, **한국 세법** 기반 세금 계산 및 연말정산 시뮬레이션을 하나의 대시보드에서 수행할 수 있습니다.

## 주요 기능

### 1. 예산 및 가계부
- **예산 설정:** 월별/카테고리별 예산 설정
- **거래 기록:** 실지출 및 수입 기록
- **지출 예정:** 미래 지출 계획 입력 및 확정
- **카드별 조회:** 특정 카드의 소비 내역만 필터링
- **날짜 범위 조회:** 원하는 기간의 거래 내역 조회

### 2. 고정 비용 관리
- **고정 비용 등록:** 월세, 통신비, 구독료 등 매월 고정 지출 관리
- **자동 소비 추가:** 고정 비용 등록 시 해당 월의 거래 내역에 자동 추가
- **결제일 관리:** 매월 결제일(dayOfMonth) 설정

### 3. 자산 관리
- **순자산 대시보드:** 총 자산 - 부채 = 순자산 확인
- **자산 유형:** 현금, 예적금, 주식, 부채 관리
- **투자 수익률:** 주식 매수가 vs 현재가 비교

### 4. 세금 및 연말정산
- **한국 세법 적용:** 국내 세법 기준 로직
- **연말정산 시뮬레이션:** 급여, 신용카드/체크카드 사용액 기반 공제액 계산
- **절세 가이드:** "남은 기간 체크카드를 더 사용하세요" 등 추천
- **주식 양도소득세:** 매도 차익에 대한 세금 계산

## 기술 스택

### 백엔드
- **언어:** Java 17+
- **프레임워크:** Spring Boot 3.x
- **데이터베이스:** MySQL 8.0 (Docker)
- **ORM:** Spring Data JPA (Hibernate)
- **빌드 도구:** Gradle

### 프론트엔드
- **프레임워크:** React (Vite)
- **언어:** TypeScript
- **스타일링:** Tailwind CSS
- **HTTP 클라이언트:** Axios

## 프로젝트 구조

```
personal_account/
├── backend/                 # Spring Boot 애플리케이션
│   ├── src/main/java/
│   │   ├── controller/      # API 엔드포인트
│   │   ├── service/         # 비즈니스 로직
│   │   ├── repository/      # 데이터 접근
│   │   ├── domain/          # JPA 엔티티
│   │   └── dto/             # 데이터 전송 객체
│   └── ...
├── frontend/                # React 애플리케이션
│   ├── src/
│   │   ├── pages/           # Dashboard, Budget, Assets, Tax
│   │   ├── components/      # 재사용 UI 컴포넌트
│   │   ├── api/             # API 서비스
│   │   └── types/           # TypeScript 타입 정의
│   └── ...
├── docker-compose.yml       # 데이터베이스 설정
└── ...
```

## 시작하기

### 사전 요구사항
- Java 17 이상
- Node.js v18 이상
- Docker & Docker Compose

### 1. 데이터베이스 실행
```bash
docker-compose up -d
```

### 2. 백엔드 실행
```bash
cd backend
./gradlew bootRun
```
서버: `http://localhost:8080`

### 3. 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
```
UI: `http://localhost:5173`

## API 엔드포인트

### 거래 내역 (Transactions)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/transactions?startDate=&endDate=` | 기간별 조회 |
| GET | `/api/transactions/by-card/{cardId}` | 카드별 조회 |
| POST | `/api/transactions` | 거래 생성 |
| PUT | `/api/transactions/{id}` | 거래 수정 |
| DELETE | `/api/transactions/{id}` | 거래 삭제 |
| PATCH | `/api/transactions/{id}/confirm` | 거래 확정 |

### 고정 비용 (Recurring Transactions)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/recurring` | 전체 조회 |
| POST | `/api/recurring` | 생성 (자동으로 소비에 추가) |
| DELETE | `/api/recurring/{id}` | 삭제 |

## 로드맵

- [x] 예산 관리 기본 기능
- [x] 거래 내역 기록 및 조회
- [x] 고정 비용 → 소비 자동 추가
- [x] 카드별/기간별 거래 필터링
- [ ] 고정 비용 월별 자동 생성 (스케줄러)
- [ ] 소비/예산 → 연말정산 자동 연동
- [ ] 거래 → 자산 잔액 자동 연동

---
*개인 재무 관리 및 세금 효율화를 위해 제작되었습니다.*
