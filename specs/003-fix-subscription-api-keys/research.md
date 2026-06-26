# Research: fix-subscription-api-keys

로컬 기동 시 `.env` 파일 내의 API Key들이 백엔드 프로세스에 주입되지 않는 이슈를 진단하고, 이를 자동으로 로딩하도록 설계하기 위한 조사 결과입니다.

## 1. 문제 분석 (Diagnosis)

- **원인**: 백엔드 프로세스 가동 시 사용된 `./gradlew :backend:bootRun` 명령어는 호스트(Terminal) 쉘 환경에 `export` 상태로 정의되지 않은 로컬 `.env` 환경 변수를 직접 참조하지 못합니다.
- **결과**: `ApplyhomeClient` 및 `LhClient` 생성자에 `@Value("${applyhome.api-key}")` 형태로 주입될 때 값의 누락이 발생하여, 프론트엔드로 전달되는 응답 JSON의 `apiKeyConfigured` 필드가 `false`가 되고 UI에 "API 키 미설정" 경고 카드가 표시됩니다.

## 2. 해결 대안 비교 (Alternatives Considered)

| 대안 | 장점 | 단점 | 선택 여부 |
|------|------|------|----------|
| **1. Gradle `bootRun` 태스크 내 .env 자동 로드 로직 구현** | IDE, 에이전트, CLI 등 어떤 환경에서 `bootRun`을 직접 실행해도 `.env`의 환경 변수가 투명하게 주입됨. 외부 의존성 없음. | Gradle 빌드 스크립트(`build.gradle`) 수정 필요. | **선택 (Recommended)** |
| **2. gradle-dotenv-plugin 플러그인 의존성 추가** | 기존에 검증된 외부 플러그인으로 로딩 구현. | 외부 플러그인 의존성이 추가되며 라이브러리 추가 비용이 발생. | 기각 |
| **3. start.sh를 통해서만 구동하도록 강제** | 추가 코드가 불필요함. | 에이전트의 자동 테스트나 개발자의 단독 백엔드 빌드 실행 시 실수를 유발하기 쉬움. | 기각 |

## 3. 최종 결정 및 아키텍처 (Final Decision)

- **선택된 방안**: **대안 1 (Gradle `bootRun` 태스크 내 .env 자동 로딩 구현)**
- **구체적인 구현 내용**:
  - `backend/build.gradle` 파일 내의 `bootRun` 태스크를 확장하여, 프로젝트 루트 폴더에 `.env` 파일이 존재할 경우 한 줄씩 읽어 `bootRun.environment` 맵에 Key-Value로 추가하는 로직을 Gradle Groovy DSL로 내장합니다.
  - 이 설정을 통해 별도 환경 세팅 없이도 `./gradlew :backend:bootRun` 실행 시 자동으로 `.env` 값을 주입받아 API 키 미설정 문제를 영구히 해결합니다.
