# Research: add-sh-gh-subscriptions

SH 및 GH 단독 청약 API의 부재에 따라, 국토교통부 마이홈포털 오픈 API를 통합 활용하여 두 공사의 청약 정보를 동적으로 수집·가공하기 위한 리서치 결과입니다.

## 1. API 상세 분석 (My Home Portal API)

- **제공 기관**: 국토교통부 (마이홈포털)
- **Base URL**: `http://apis.data.go.kr/1613000/HWSPR02`
- **대상 엔드포인트**:
  - 임대주택: `/rsdtRcritNtcList` (공공임대주택 모집공고 조회)
  - 분양주택: `/ltRsdtRcritNtcList` (공공분양주택 모집공고 조회)
- **주요 요청 변수**:
  - `serviceKey`: 공공데이터포털 일반 인증키 (디코딩 버전 권장)
  - `PG_SZ` / `PAGE`: 페이징 크기 및 페이지 번호 (최근 공고 100건 수집을 위해 `PG_SZ=100`, `PAGE=1` 사용)
- **응답 형식**:
  - 마이홈 API는 기본적으로 XML 형식으로 전달될 확률이 높습니다. 본 프로젝트 백엔드(`build.gradle`)에 이미 `jackson-dataformat-xml` 의존성이 정의되어 있으므로, Jackson `XmlMapper` 또는 `RestClient`를 활용해 XML을 손쉽게 JSON/JsonNode 객체로 디시리얼라이즈하여 파싱을 일원화합니다.

## 2. 공급기관 분류 방식 (Filtering & Categorization)

API 응답 아이템 내의 **`suplyInsttNm` (공급기관명)** 값을 검사하여 SH 및 GH를 동적으로 분류합니다.

| 공급기관명 (`suplyInsttNm`) | 대상 분류 | 필터링 기준 |
|-------------------------|-------|-----------|
| `"서울주택도시공사"` (혹은 `"SH"`) | **SH** | `suplyInsttNm.contains("서울주택도시공사")` |
| `"경기주택도시공사"` (혹은 `"GH"`) | **GH** | `suplyInsttNm.contains("경기주택도시공사")` |

## 3. 백엔드 연동 설계 (Backend Design)

- **`MyHomeClient.java` 신규 구현**:
  - LHClient 패턴을 벤치마킹하여 `MyHomeClient` 빈을 구현하고, `MYHOME_API_KEY`를 주입받아 임대/분양 엔드포인트를 순차 호출하여 `JsonNode` 목록을 합쳐 반환합니다.
- **`MyHomeSubscriptionService.java` 신규 구현**:
  - 수집된 `JsonNode` 목록 중 타겟 지역(서울 및 경기도 4개 시군구)에 해당하는 항목들을 선별하고, 공급기관명에 따라 SH와 GH 응답 객체(`SubscriptionsResponse`)로 각각 나누어 제공합니다.
- **`SubscriptionController.java` 엔드포인트 확장**:
  - `@GetMapping("/sh/today")` 및 `@GetMapping("/gh/today")` 를 추가하여 프론트엔드가 이를 즉시 수집할 수 있도록 개방합니다.
