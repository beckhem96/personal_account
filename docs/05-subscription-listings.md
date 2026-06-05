# 5. 청약 일정 조회 (수도권 5개 지역, 접수 예정+진행중)

> 갱신 1: 초기엔 "오늘 접수중"만 표시했으나, 대부분의 날에 화면이 비는 문제로 **접수 예정(시작 전) + 진행중**(= 마감일이 지나지 않은 단계)을 모두 표시하도록 넓혔다. 카드에 `시작 D-N`(예정) / `마감 D-N`(진행중) 배지로 구분. 판정 기준: `isOpenOrUpcoming(today, begin, end)` = 마감일이 오늘 이후. LH 탭도 동일하게 접수마감 제외 방식.
>
> 갱신 2: **특별공급(SPECIAL)** 단계 추가 — 응답의 `SPSPLY_RCEPT_BGNDE`/`SPSPLY_RCEPT_ENDDE`로 판정, 1·2순위·무순위와 함께 단계 칩/섹션으로 표시. **대상 지역 확장**: 서울 + 의정부/남양주/하남/구리 **+ 용인/수원/김포**. (`TARGET_DISTRICTS`에 추가, 청약홈·LH 양쪽 적용)
>
> 갱신 3: **접수중 / 공고중 상태 필터** 추가(프론트). 백엔드는 이미 공고중(접수 시작 전)도 반환하므로, 화면에 "상태" 칩(접수중/공고중, 기본 둘 다)과 카드 상태 배지를 더해 나눠 볼 수 있게 함. 청약홈은 단계별 접수 시작일(`today < begin`→공고중)로, LH는 `PAN_SS`(접수중 외 전부 공고중 취급)로 판정. 청약홈·LH 동일 적용.

## 배경

청약홈(한국부동산원)에서 매일 새 공고를 직접 확인해야 하는 부담을 줄이기 위해, 사용자가 관심을 두는 5개 지역(서울 전역 + 경기 의정부/남양주/하남/구리)의 **오늘 접수 진행 중인 청약**만 한 화면에 모아 보는 기능. 1순위 / 2순위 / 무순위(잔여세대)를 구분해서 표시.

## 사용자 결정 사항

| 항목 | 결정 |
|------|------|
| 청약 종류 범위 | APT 일반청약(1·2순위) + APT 무순위/잔여세대 (오피스텔·도시형 제외) |
| 부가 기능 | 단순 조회만 (즐겨찾기/알림 없음) |
| 지역 | 서울특별시 + 경기 의정부/남양주/하남/구리 (고정) |

## 데이터 소스

공공데이터포털 **한국부동산원 청약홈 분양정보 조회 서비스** (`ApplyhomeInfoDetailSvc`).

`MOLIT_API_KEY`(아파트매매 실거래가)와 **동일 키**를 사용한다 — data.go.kr 사용자 단위 단일 키이므로 활용신청만 별도. `.env`에 `APPLYHOME_API_KEY`로 분리해 두면 둘 중 한쪽만 활용신청 상태여도 명확.

두 엔드포인트:
- 일반 APT: `/getAPTLttotPblancDetail`
- 잔여세대: `/getRemndrLttotPblancDetail`

### 핵심 응답 필드 (docx 명세 기준)

```
HOUSE_MANAGE_NO         주택관리번호
HOUSE_NM                단지명
HOUSE_SECD_NM           주택구분 (APT/공공주택 등)
SUBSCRPT_AREA_CODE_NM   공급지역 (예: "서울특별시", "경기")
HSSPLY_ADRES            공급위치 (시군구 매칭에 사용)
RCRIT_PBLANC_DE         모집공고일
TOT_SUPLY_HSHLDCO       총 공급세대수
PBLANC_URL              청약홈 공식 공고 페이지 URL
```

접수 기간 — 1순위/2순위 각각 3개 영역으로 분리:

```
GNRL_RNK1_CRSPAREA_RCPTDE / _ENDDE   1순위 해당지역
GNRL_RNK1_ETC_GG_RCPTDE   / _ENDDE   1순위 경기지역
GNRL_RNK1_ETC_AREA_RCPTDE / _ENDDE   1순위 기타지역
GNRL_RNK2_CRSPAREA_RCPTDE / _ENDDE   2순위 해당지역
GNRL_RNK2_ETC_GG_RCPTDE   / _ENDDE   2순위 경기지역
GNRL_RNK2_ETC_AREA_RCPTDE / _ENDDE   2순위 기타지역
```

사용자 거주지에 따라 접수일이 달라서, **어느 영역이라도 오늘 진행 중이면 해당 순위 활성**으로 본다.

잔여세대는 `SUBSCRPT_RCEPT_BGNDE/ENDDE` 단일 영역.

## 백엔드 구성

### `ApplyhomeClient`
`backend/src/main/java/org/example/account/client/ApplyhomeClient.java`

- `isAvailable()` — 키 빈 값이면 false (호출 우회)
- `fetchAptList()`, `fetchRemainderList()` — `JsonNode` 트리 반환 (필드명 유연성)
- 서버측 필터: `cond[RCRIT_PBLANC_DE::GTE]=오늘-60일` — 최근 모집공고만 가져와 응답 크기 축소
- `perPage=1000`

#### URI 인코딩 우회 (핵심 함정)

Spring `RestClient.queryParam()`은 쿼리 값의 `+`/`/`/`=` 같은 reserved 문자를 인코딩하지 않는다. data.go.kr 인증키는 base64라 `+`와 `/`를 포함하는데, 서버가 `+`를 공백으로 해석해 **"등록되지 않은 인증키"** 에러가 발생.

해결: `URLEncoder`로 직접 인코딩한 raw 쿼리 스트링을 `URI.create()`로 만들어 전달.

```java
String query = "serviceKey=" + URLEncoder.encode(apiKey, UTF_8)
        + "&page=1&perPage=" + PER_PAGE + "&returnType=JSON"
        + "&" + URLEncoder.encode("cond[RCRIT_PBLANC_DE::GTE]", UTF_8) + "=" + since;
URI uri = URI.create(baseUrl + path + "?" + query);
restClient.get().uri(uri).retrieve().body(JsonNode.class);
```

### `SubscriptionService`
`backend/src/main/java/org/example/account/service/SubscriptionService.java`

```
findActiveToday():
  1. ApplyhomeClient.isAvailable() false → apiKeyConfigured=false 응답
  2. 일반 + 잔여세대 응답 합쳐 SubscriptionItem 변환
  3. 5개 지역 필터 (regionLabel.contains("서울") OR address.contains("의정부"/"남양주"/"하남"/"구리"))
  4. 진행 중인 단계 식별 (FIRST/SECOND/REMAINDER)
  5. 단계별 그룹핑하여 응답
```

날짜 파싱은 `yyyy-MM-dd` / `yyyy.MM.dd` / `yyyyMMdd` 세 형식 시도 (응답 포맷이 일정치 않음).

청약홈 공고 URL은 응답의 `PBLANC_URL` 우선, 없으면 `houseManageNo`로 조립.

### DTO / Enum
- `SubscriptionRank` (`FIRST | SECOND | REMAINDER`)
- `SubscriptionItem` (단지명, 위치, 단계별 접수 시작/종료, 활성 단계, 공고 URL …)
- `SubscriptionsResponse` (`asOf`, `apiKeyConfigured`, `firstRank`, `secondRank`, `remainder`)

### Controller
`GET /api/subscriptions/today` → `SubscriptionsResponse`

## 프론트엔드

### 페이지 `Subscriptions.tsx`
- 헤더: 종 아이콘 + "오늘 접수중인 청약" + 새로고침 버튼
- 필터: 지역 5칩(기본 모두 선택) + 순위 3칩 토글
- 결과: 단계별 섹션(1순위/2순위/무순위) → 카드 그리드
  - 단지명, 주소, 주택구분 배지, 접수 시작/종료 + **D-N 배지**, 총 공급세대수
  - "청약홈에서 보기" 외부 링크 (응답의 `PBLANC_URL` 사용)
- 빈 상태: "선택한 조건에 해당하는 청약이 없습니다"
- API 키 미설정 시 안내 카드 (`.env`에 `APPLYHOME_API_KEY` 설정 가이드)

### 라우팅
- `App.tsx` — `<Route path="/subscriptions">`
- `Layout.tsx` — 사이드바에 `Bell` 아이콘 + "청약 일정"
- `api/services.ts` — `getTodaySubscriptions()`
- `types/index.ts` — `SubscriptionRank`, `SubscriptionItem`, `SubscriptionsResponse`

## 검증

### 단위 테스트 — `SubscriptionServiceTest`
6개 시나리오:
1. 키 미설정 → 빈 응답 + `apiKeyConfigured=false`
2. 지역 필터 (서울/남양주는 통과, 부산/수원은 차단)
3. 1순위 해당지역 접수일이 오늘이면 FIRST 활성
4. **거주지영역 달라도 기타지역 접수일이 오늘이면 FIRST 활성** (3영역 OR 검증)
5. 모든 접수기간이 과거이면 활성 단계 없음
6. 일반/무순위 API 두 곳 모두 호출하고 그룹별로 분리

### 수동 시나리오
1. `.env`에 `APPLYHOME_API_KEY` 없는 상태로 `/subscriptions` → 키 미설정 안내 + 빈 리스트
2. 키 설정 후 진입 → 오늘 접수중인 5개 지역 청약이 단계별 카드로 표시
3. 청약홈 공고 링크 클릭 → `applyhome.co.kr` 새 탭으로 열림
4. 지역/순위 칩 토글 → 클라이언트 측 즉시 필터링

## 알려진 한계 / 향후 개선

- **시간대**: `LocalDate.now()` 사용. 서버 JVM이 `Asia/Seoul`이라는 전제 (`application.yml`에서 설정).
- **지역 매칭은 단순 contains**: 주소가 "별내신도시 A-1BL"처럼 시군구가 빠진 경우 놓침. 누락 사례 발견 시 보강.
- **캐시 없음**: 1순위/2순위/무순위 모두 합쳐 일반 + 잔여세대 두 번 호출. 호출량이 늘어나면 30분 TTL 캐시 도입 검토.
- **알림 기능 미구현**: 새 공고가 등록됐을 때 푸시 알림은 향후 과제.
