# Data Model & Configuration: fix-subscription-api-keys

본 기능 개발은 데이터베이스 스키마(MySQL) 변경 사항을 유발하지 않으며, 백엔드 기동 환경 변수 및 설정 데이터 바인딩 구조의 개선만 포함합니다.

## 1. 환경 변수 매핑 테이블 (Environment Variables)

백엔드 애플리케이션의 설정 속성(`application.yml`)은 호스트 OS 환경 변수에서 아래와 같이 주입받습니다.

| 설정 항목 (YML Path) | 환경 변수 Key (OS Env) | 용도 |
|-------------------|----------------------|-----|
| `applyhome.api-key` | `APPLYHOME_API_KEY` | 한국부동산원 청약홈 API 인증키 |
| `lh.api-key` | `LH_API_KEY` | LH 한국토지주택공사 공고 API 인증키 |
| `molit.api-key` | `MOLIT_API_KEY` | 국토교통부 아파트실거래가 API 인증키 |

## 2. API 응답 객체 정보 (Data Transfer Objects)

- **`SubscriptionsResponse`** (`org.example.account.dto.SubscriptionsResponse`):
  - `apiKeyConfigured` (boolean): `ApplyhomeClient.isAvailable()`이 반환하는 값에 따라 `true`/`false`로 지정되어 프론트엔드로 전달됩니다.
- **`LhNoticesResponse`** (`org.example.account.dto.LhNoticesResponse`):
  - `apiKeyConfigured` (boolean): `LhClient.isAvailable()`이 반환하는 값에 따라 `true`/`false`로 지정되어 프론트엔드로 전달됩니다.
