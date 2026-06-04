# 6. LH 공공분양·임대 청약 조회

## 배경

기존 청약 일정 페이지는 청약홈(한국부동산원)의 민영/공공분양 APT(일반·무순위)만 다뤘다. 사용자는 LH(한국토지주택공사)가 공급하는 **공공분양·신혼희망타운·공공임대** 청약도 같은 5개 지역(서울 + 의정부/남양주/하남/구리)에서 추적하고 싶어 한다. 청약 페이지에 **"LH 공공" 탭**을 추가하고, LH 분양임대공고문 조회 API를 연동했다.

이 작업은 "공공청약(LH/SH) + 복잡한 세금 계산" 로드맵의 1단계다. SH(서울주택도시공사)는 공공데이터 API가 빈약해 후순위로 미뤘다.

## 사용자 결정 사항

| 항목 | 결정 |
|------|------|
| 공급유형 범위 | 분양 전체 + 신혼희망타운 + 주요 임대(행복주택·국민임대·영구임대). 매입임대·전세임대 제외 |
| 지역 필터 | 기존 5개 지역 동일 (서울 + 의정부/남양주/하남/구리) |
| 부가 기능 | 단순 조회 (즐겨찾기/알림 없음) |

## 데이터 소스

공공데이터포털 **한국토지주택공사_분양임대공고문 조회 서비스** (`lhLeaseNoticeInfo1`)
`https://apis.data.go.kr/B552555/lhLeaseNoticeInfo1/lhLeaseNoticeInfo1`

- data.go.kr 공통 키 사용(기존 `MOLIT_API_KEY`와 동일 값). **API마다 개별 활용신청** 필요 → `.env`에 `LH_API_KEY`로 분리. (2026-06-04 활용신청 승인됨)
- 데이터 포맷: JSON. 인증키는 URL Encode 형태로 전달.

### 요청 파라미터 (docx 명세 기준)

| 파라미터 | 의미 | 사용 |
|----------|------|------|
| `serviceKey` | 인증키(URL Encode) | 필수 |
| `PG_SZ` / `PAGE` | 페이지 크기/번호 | `PG_SZ=100` |
| `UPP_AIS_TP_CD` | 공고유형코드 | **05 분양주택 / 06 임대주택 / 39 신혼희망타운** (01 토지·13 주거복지·22 상가 제외) |
| `CNP_CD` | 지역코드 | **11 서울 / 41 경기** |
| `PAN_SS` | 공고상태코드 | 미사용(상태는 응답에서 필터) |

> **주의**: 신혼희망타운은 05 분양 하위가 아니라 **별도 코드 39**. 그래서 05/06/39 × 11/41 = **6회 호출**해 합산한다.

### 응답 (dsList) 주요 필드

```
PAN_ID          공고ID
PAN_NM          공고명
UPP_AIS_TP_CD   상위유형 (05/06/39)
UPP_AIS_TP_NM   상위유형명 (분양주택/임대주택)
AIS_TP_CD_NM    세부유형명 (분양주택/행복주택/국민임대/신혼희망타운 …)
CNP_CD_NM       지역명 (서울특별시/경기도 …)
PAN_NT_ST_DT    공고게시일 (예: 2020.04.27)
CLSG_DT         공고마감일 (예: 2020.04.28)
PAN_SS          공고상태 (공고중/접수중/접수마감/정정공고중/상담요청)
DTL_URL         공고 상세 URL (직접 제공)
```

응답 래핑: `[ {dsSch:[…]}, {resHeader:[…], dsList:[…]} ]`. `dsList`가 공고 행 배열.

## 백엔드 구성

### `LhClient`
`backend/.../client/LhClient.java`

- `isAvailable()` — 키 빈 값이면 false
- `fetchAll()` — 05/06/39 × 11/41 (6회) 호출해 행 합산
- **인증키 인코딩 우회**: Spring `RestClient.queryParam()`이 `+`/`/`를 인코딩하지 않아 키가 깨지는 문제를 `URLEncoder` + `URI.create()`로 우회
- **응답 파싱**: 트리를 재귀 탐색해 `PAN_NM`/`PAN_ID`/`AIS_TP_CD_NM`를 가진 객체 배열(=`dsList`)을 찾아 반환 → `dsSch`/`resHeader`는 자연히 제외, 래핑 구조 변동에도 견딤
- 호출 실패(미신청·지역차단 등)는 빈 목록 + 경고 로그로 흡수

### `LhSubscriptionService`
`backend/.../service/LhSubscriptionService.java`

```
findActiveToday():
  1. isAvailable() false → apiKeyConfigured=false 빈 응답
  2. fetchAll() 행을 LhNoticeItem으로 변환하며:
       - PAN_SS == 접수마감 이거나 CLSG_DT가 지난 공고는 제외
       - category: UPP_AIS_TP_CD == 06 → RENT, 그 외(05/39) → SALE
  3. 지역 필터: (CNP_CD_NM + PAN_NM)에 서울/의정부/남양주/하남/구리 contains
  4. PAN_ID 기준 중복 제거
  5. 임대(RENT)는 AIS_TP_CD_NM이 행복주택/국민임대/영구임대 부분일치만 통과
       → 매입임대·전세임대·장기전세 등 제외
  6. 분양(SALE)/임대(RENT) 그룹으로 분리 반환
```

- 분양(05)·신혼희망타운(39)은 유형 화이트리스트 없이 전부 포함.
- 상세 URL은 응답의 `DTL_URL`(없으면 `DTL_URL_MOB`)을 그대로 사용.
- 날짜 포맷 `yyyy.MM.dd` / `yyyy-MM-dd` / `yyyyMMdd` 모두 시도.

### DTO / Enum / Controller / 설정
- `LhNoticeItem`(record) — panId, name, supplyTypeName, category, regionLabel, noticeStatus, noticeDate(게시일), rcptBegin(목록 API엔 없어 null), rcptEnd(마감일), detailUrl
- `LhNoticesResponse` — asOf, apiKeyConfigured, sale, rent
- `LhSupplyCategory` enum (`SALE`, `RENT`)
- `GET /api/subscriptions/lh/today` → `LhNoticesResponse`
- `RestClientConfig`에 `lhRestClient` 빈, `application.yml`에 `lh` 블록, `.env`에 `LH_API_KEY`

## 프론트엔드

`frontend/src/pages/Subscriptions.tsx`를 **탭 구조**로 리팩터링:
- 상단 탭 토글: `청약홈 (APT)` ↔ `LH 공공`
- 기존 화면은 `ApplyhomeView`로 분리, 공통 UI(Toolbar/ChipGroup/ToggleChip/안내 카드/EmptyNotice/DetailLink) 추출해 재사용
- `LhView`: 지역 5칩 + **공급유형 동적 칩**(응답의 실제 `AIS_TP_CD_NM` 값으로 구성 — `분양주택`/`행복주택` 등 실제 명칭과 어긋나지 않게), **분양/임대 섹션**별 카드
  - 카드: 공고명, 지역, 세부유형 배지, 공고일, 마감일 + D-N, 공고상태 배지, "LH 청약센터에서 보기"(`DTL_URL`)
  - LH API 별도 활용신청 안내 배너 상시 노출
- `types/index.ts`에 `LhSupplyCategory`/`LhNoticeItem`/`LhNoticesResponse`, `services.ts`에 `getLhSubscriptions()` 추가

## 검증

### 단위 테스트 — `LhSubscriptionServiceTest` (5건)
1. 키 미설정 → 빈 응답 + `apiKeyConfigured=false`
2. 임대유형 필터 — 행복주택/국민임대 통과, 매입임대/전세임대 제외
3. 지역 필터 — 서울/하남 통과, 부산/대구 제외
4. 접수마감 상태 또는 마감일 지난 공고 제외
5. 신혼희망타운(39)은 분양 그룹, 임대는 임대 그룹으로 분리 + PAN_ID 중복 제거

```bash
./gradlew :backend:test --tests "*LhSubscriptionServiceTest"   # 통과
```

### 라이브 검증 제약 (중요)
- **개발 환경(클라우드)에서는 `apis.data.go.kr`가 해외 IP를 지역 차단(HTTP 403)** 한다. 같은 키의 MOLIT 실거래가도 이 환경에선 403. 반면 `api.odcloud.kr`(청약홈)는 차단 안 함.
- 따라서 LH 연동의 라이브 검증은 **국내망(사용자 PC)에서 백엔드 재시작 후** `/api/subscriptions/lh/today` 호출로 수행해야 한다. 구현은 docx 공식 명세 + 샘플 응답에 맞춰 작성됨.

### 알려진 제약 / 후속
- **프론트 빌드 선결 이슈(본 작업과 무관)**: `npm run build`가 `StockAnalysis.tsx`/`services.ts`의 누락 타입(`MyStock` 등)으로 이미 실패 중. LH 관련 파일은 tsc 통과. 별도 복구 과제.
- **시군구 매칭 한계**: 경기 공고는 지역명(경기도)+공고명 contains로 의정부/남양주/하남/구리 판별. 공고명에 시군구가 없으면 누락 → 로드맵 A2(공급정보 API)에서 지구명 기반 보강.
- **접수 시작일 부재**: 목록 API는 게시일·마감일만 제공. 접수 시작일은 공급정보/상세 API(A2)에서 보강.

## 로드맵 내 위치

Track A(공공/임대 청약) A1. 다음 후보: A2(LH 공급정보 연동), Track B의 부동산 양도세/종부세 계산기.
