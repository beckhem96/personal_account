# 6. LH 공공분양·임대 청약 조회

## 배경

기존 청약 일정 페이지는 청약홈(한국부동산원)의 민영/공공분양 APT(일반·무순위)만 다뤘다. 사용자는 LH(한국토지주택공사)가 공급하는 **공공분양·공공임대** 청약도 같은 5개 지역(서울 + 의정부/남양주/하남/구리)에서 추적하고 싶어 한다. 이를 위해 청약 페이지에 **"LH 공공" 탭**을 추가하고, LH 분양임대공고문 조회 API를 연동했다.

이 작업은 "공공청약(LH/SH) + 복잡한 세금 계산" 로드맵의 1단계다. SH(서울주택도시공사)는 공공데이터 API가 빈약(공사계약현황 수준)해 후순위로 미뤘다.

## 사용자 결정 사항

| 항목 | 결정 |
|------|------|
| 공급유형 범위 | 분양 전체 + 주요 임대(행복주택·국민임대·영구임대). 매입임대·전세임대 제외 |
| 지역 필터 | 기존 5개 지역 동일 (서울 + 의정부/남양주/하남/구리) |
| 부가 기능 | 단순 조회 (즐겨찾기/알림 없음) |

## 데이터 소스

공공데이터포털 **한국토지주택공사_분양임대공고문 조회 서비스**
`https://apis.data.go.kr/B552555/lhLeaseNoticeInfo1/lhLeaseNoticeInfo1`

- data.go.kr 공통 키 사용(기존 `MOLIT_API_KEY`와 **동일 값**). 단, **API마다 개별 활용신청**이 필요해 `.env`에 `LH_API_KEY`로 분리.
- 요청 파라미터: `serviceKey`, `PG_SZ`, `PAGE`, `UPP_AIS_TP_CD`(상위공고유형 — **05=분양, 06=임대**)
- 응답 구조: `[ {resHeader:[...]}, {dsList:[...]} ]` 형태의 배열. `dsList` 안에 공고 행 배열.

### 주요 응답 필드 (부분일치/폴백으로 흡수)

```
PAN_ID            공고ID
PAN_NM            공고명
AIS_TP_CD_NM      공급유형명 (공공분양/행복주택/국민임대 …)
UPP_AIS_TP_CD     상위유형 (05 분양 / 06 임대)
AIS_TP_CD         유형코드
CCR_CNNT_SYS_DS_CD 연계시스템구분 (상세 URL 조립용)
CNP_CD_NM         지역(시도)
PAN_SS_NM         공고상태명
CLSG_DT           마감일
```

> 청약홈 때와 동일하게, 필드명이 유동적이라 **`JsonNode` 유연 파싱 + 폴백 필드명**으로 처리. 실제 응답으로 미세 조정 예정.

## 백엔드 구성

### `LhClient`
`backend/.../client/LhClient.java`

- `isAvailable()` — 키 빈 값이면 false
- `fetchSaleNotices()`(UPP=05), `fetchRentNotices()`(UPP=06)
- **인증키 인코딩 우회**: Spring `RestClient.queryParam()`이 `+`/`/`를 인코딩하지 않아 키가 깨지는 문제를 `URLEncoder` + `URI.create()`로 우회 (ApplyhomeClient와 동일)
- **응답 파싱**: 트리를 재귀 탐색해 `PAN_NM`/`PAN_ID`/`AIS_TP_CD_NM`를 가진 객체 배열(=`dsList`)을 찾아 행으로 반환 → 응답 래핑 구조가 바뀌어도 견딤
- 활용신청 미완료 시 본문에 `Forbidden`(403)이 내려옴 → 예외를 잡아 빈 목록 + 경고 로그로 흡수

### `LhSubscriptionService`
`backend/.../service/LhSubscriptionService.java`

```
findActiveToday():
  1. isAvailable() false → apiKeyConfigured=false 빈 응답
  2. 분양/임대 공고 조회 → LhNoticeItem 변환
  3. 접수중 판정 isReceivable(today, begin, end):
       마감일 지났으면 제외 / 시작일 안 됐으면 제외 / 일정 미상이면 노출(상세 링크로 확인)
  4. 임대는 RENT_ALLOWED_TYPES(행복주택·국민임대·영구임대) 부분일치만 통과 → 매입·전세임대 제외
  5. 지역 필터: (지역명 + 공고명)에 서울/의정부/남양주/하남/구리 contains
  6. 분양/임대 그룹으로 분리 반환
```

- 분양(UPP=05)은 유형 화이트리스트를 적용하지 않고 전부 포함(사용자 의도: 분양 전체).
- LH 상세 URL은 응답의 `DTL_URL` 우선, 없으면 `panId`+`ccrCnntSysDsCd`+`uppAisTpCd`+`aisTpCd`로 `selectWrtancInfo.do` 링크 조립.

### DTO / Enum / Controller
- `LhNoticeItem`(record), `LhNoticesResponse`(asOf, apiKeyConfigured, sale, rent)
- `LhSupplyCategory` enum (`SALE`, `RENT`)
- `GET /api/subscriptions/lh/today` → `LhNoticesResponse` (기존 SubscriptionController에 추가)
- `RestClientConfig`에 `lhRestClient` 빈, `application.yml`에 `lh` 블록 추가

## 프론트엔드

`frontend/src/pages/Subscriptions.tsx`를 **탭 구조**로 리팩터링:
- 상단 탭 토글: `청약홈 (APT)` ↔ `LH 공공`
- 기존 청약홈 화면은 `ApplyhomeView`로 분리, 공통 UI(Toolbar/ChipGroup/ToggleChip/안내 카드/EmptyNotice/DetailLink) 추출해 재사용
- `LhView`: 지역 5칩 + 공급유형 5칩(공공분양/신혼희망타운/행복주택/국민임대/영구임대) 필터, **분양/임대 섹션**별 카드
  - 카드: 공고명, 지역, 유형 배지, 공고상태, 접수기간(있으면 D-N), "LH 청약센터에서 보기" 링크
  - LH 탭에는 "API 별도 활용신청 필요" 안내 배너 상시 노출
- `types/index.ts`에 `LhSupplyCategory`/`LhNoticeItem`/`LhNoticesResponse`, `services.ts`에 `getLhSubscriptions()` 추가

## 검증

### 단위 테스트 — `LhSubscriptionServiceTest` (5건)
1. 키 미설정 → 빈 응답 + `apiKeyConfigured=false`
2. 임대유형 필터 — 행복주택/국민임대 통과, 매입임대/전세임대 제외
3. 지역 필터 — 서울/하남 통과, 부산/대구 제외
4. 접수 마감 지난 공고 제외
5. 분양/임대 그룹 분리

```bash
./gradlew :backend:test --tests "*LhSubscriptionServiceTest"   # 통과
./gradlew :backend:build                                        # 통과
```

### 알려진 제약 / 후속 작업
- **활용신청 필요**: LH API는 data.go.kr에서 별도 활용신청 승인 전까지 `Forbidden`(403). 현재 키는 아파트매매·청약홈만 신청된 상태 → 승인 후 라이브 검증 필요.
- **프론트 빌드 선결 이슈(본 작업과 무관)**: `npm run build`가 `StockAnalysis.tsx`/`services.ts`의 `MyStock`·`StockAnalysis`·`MarketOutlookResponse` 등 **누락된 타입**으로 인해 이미 실패 중. LH 관련 파일(`Subscriptions.tsx`, 신규 타입)은 tsc 통과. StockAnalysis 타입 복구는 별도 과제.
- **시군구 매칭 한계**: 지역명(시도)+공고명 contains 방식. 공고명에 시군구가 없으면 누락 → 로드맵 A2(공급정보 API)에서 지구명 기반으로 보강.
- **접수일정 부재 가능**: 목록 API에 접수 시작/마감일이 없으면 노출만 하고 상세 링크로 확인하도록 둠.

## 로드맵 내 위치

Track A(공공/임대 청약)의 A1. 다음 후보: A2(LH 공급정보 연동), Track B의 부동산 양도세/종부세 계산기.
