# 2. 주택 구매 비용 계산기

커밋: `f7f6397 주택 구매 비용 계산기 기능 추가`

## 배경

주택 구매 시 매매가 외에 발생하는 부대비용(취득세, 중개수수료, 법무사, 채권할인), 대출 부대비용(근저당설정비, 인지세, 감정평가수수료), 대출 상환방식별 월 납입액과 스케줄, 그리고 수도권 권역별 평균 매매가/실거래 정보를 한 화면에서 시뮬레이션하기 위한 신규 페이지.

기존 Tax 페이지(주식 양도세/연말정산)와 같은 "단순 계산기" 위치 — Asset/Transaction 도메인과 연결하지 않는다.

## 사용자 결정 사항

| 항목 | 결정 |
|------|------|
| 데이터 소스 | 국토교통부 실거래가 공공 API (data.go.kr) |
| 취득세 입력 | 1주택/다주택 + 조정대상지역 + 생애최초 모두 입력 |
| 대출 입력 | 상품군(디딤돌/보금자리/일반) 선택 + 금리/기간 직접 입력 |
| 결과 처리 | 단순 계산기 (Asset/Transaction 연계 없음) |

## 구성 요소

### 백엔드 (`backend/src/main/java/org/example/account/`)

| 분류 | 파일 |
|------|------|
| Controller | `controller/HousingController.java` — 6개 엔드포인트 |
| Service (책임별 분리) | `service/HousingTaxService.java` (취득세 매트릭스), `service/HousingAcquisitionCostService.java` (중개수수료/법무사/채권할인), `service/HousingLoanCostService.java` (근저당설정비/인지세/감평수수료), `service/HousingLoanRepaymentService.java` (3가지 상환방식 + 거치기간), `service/HousingMarketService.java` (국토부 응답 + 평균/필터) |
| Client | `client/MolitClient.java` — XML 응답 파싱, `serviceKey` 마스킹 |
| DTO (record) | `dto/Acquisition*Request/Response`, `dto/LoanCost*`, `dto/LoanRepayment*`, `dto/LoanScheduleRow`, `dto/LoanProductInfo`, `dto/ApartmentDealDto`, `dto/RegionInfo` 등 10여 개 |
| Enum | `domain/HouseCount`, `domain/RepaymentType`, `domain/LoanProductCode` |
| Resource | `src/main/resources/lawd-codes.json` — 수도권 법정동 코드 77개 (서울 25, 인천 10, 경기 42) |

### 프론트엔드

- `frontend/src/pages/Housing.tsx` — 4탭 페이지 (취득·부대비용 / 대출 부대비용 / 대출 상환 / 수도권 아파트)
- 차트: PieChart(비용 분해), LineChart(잔액 곡선), BarChart(권역 평균가)
- `frontend/src/components/Layout.tsx` — 사이드바 "Housing" 항목 (lucide `Home` 아이콘)

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/housing/acquisition-cost` | 취득세 + 부대비용 통합 |
| POST | `/api/housing/loan-cost` | 근저당·인지세·감평수수료 |
| POST | `/api/housing/loan-repayment` | 월 납입액 + 회차별 스케줄 |
| GET | `/api/housing/loan-products` | 디딤돌/보금자리/일반 메타데이터 |
| GET | `/api/housing/regions` | 수도권 권역→시군구→법정동 트리 |
| GET | `/api/housing/apartment-deals` | 국토부 API + 평균가/필터 |

## 도메인 로직 (한국 세법)

### 취득세 매트릭스
- 1주택 6억 이하 → 1%
- 1주택 6~9억 → 누진식: `(매매가 × 2/3억 − 3) / 100`
- 1주택 9억 초과 → 3%
- 다주택+조정대상지역: 2주택 8%, 3주택 이상 12%
- 농특세: 전용면적 85㎡ 초과 시 0.2%
- 지방교육세: 취득세의 10%
- 생애최초: 200만원 한도 감면

### 대출 상환 (PMT 표준식, 원리금균등)
```
월납입액 = P × r × (1+r)^n / ((1+r)^n − 1)
```
- 거치기간 동안은 이자만 납부, 거치 종료 후 잔여 기간으로 원리금 재계산
- 마지막 회차에서 반올림 잔액 보정 → 잔액 정확히 0으로 수렴

### 부대비용
- 중개수수료: 가격대별 상한요율 (~0.4%, 5~9억 0.5%, 9억↑ 0.9% 상한 협의)
- 법무사: `매매가 × 0.0007` (30만~150만 캡)
- 인지세: 가격대별 구간 (15만 / 35만 / 75만 등)
- 국민주택채권 할인손실: `채권매입액 × (1 − smp ratio)`, 기본 smp 0.7

## 외부 API 통합 (국토부 실거래가)

- `application.yml`의 `molit.api-key`는 `${MOLIT_API_KEY:}` env 주입 — 키 미설정 시 `MolitClient.isAvailable()`이 false 반환, 시세 조회는 빈 결과로 우회됨
- 응답이 XML이라 `jackson-dataformat-xml` 의존성 추가
- 한글 필드(`거래금액`/`전용면적`/`건축년도`)는 `XmlMapper.readTree()`로 파싱
- 금액 문자열 `"  90,000"` → `replaceAll("[ ,]", "")` 후 `BigDecimal`
- 캐싱: Caffeine `@Cacheable("apartmentDeals")`, key=`lawdCd:dealYmd`, TTL 24h

## 검증

- 단위 테스트: `HousingTaxServiceTest` (7건), `HousingLoanRepaymentServiceTest` (5건) — 모두 통과
- 수동 시나리오: 6억 1주택 + 디딤돌 35년 3.5% 원리금균등 → 월납입 ~248만, 12억 1주택 일반 주담대 30년 4.5% 5년거치 원금균등 → 9억 초과 누진 취득세, 9억 다주택+조정지역 → 취득세 8% + 농특세 0.2%

## 알려진 위험

- **국토부 키 발급 지연**: 2~3일. 키 없어도 다른 탭(취득세/대출)은 정상 동작
- **취득세율 갱신**: 2025년 말 기준. `HousingTaxService` 상단 상수에 모아 일괄 수정 가능
- **법정동 코드 stale**: `lawd-codes.json` 수동 갱신 필요
- **국민주택채권 할인율 변동**: 사용자가 "smp ratio" 수동 입력으로 우회
