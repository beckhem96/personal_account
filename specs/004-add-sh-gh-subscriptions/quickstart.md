# Quickstart Guide: add-sh-gh-subscriptions

본 가이드는 국토교통부 마이홈 API가 정상 연동되어 SH 및 GH 청약 공고 조회가 잘 동작하는지를 로컬 환경에서 수동 검증하는 절차를 설명합니다.

## 1. 전제 조건 (Prerequisites)

1. 프로젝트 루트의 `.env` 파일에 `MYHOME_API_KEY` 환경변수가 정의되어 있어야 합니다.
   ```properties
   MYHOME_API_KEY=YOUR_ACTUAL_DECODED_DATA_GO_KR_KEY
   ```
2. API 키 미주입 상태에서의 경고창 동작을 검증하려면 임시로 이 키 선언을 주석 처리(`##`)한 상태에서 서버를 시작해 봅니다.

---

## 2. 수동 검증 절차 (Manual Verification Steps)

### A. 백엔드 개별 API 검증
1. 백엔드를 기동합니다.
   ```bash
   ./gradlew :backend:bootRun
   ```
2. SH 청약 데이터 조회를 검증합니다.
   ```bash
   curl -s http://localhost:8080/api/subscriptions/sh/today
   ```
   **기대 결과**: 응답 JSON 내에 `"apiKeyConfigured": true`가 포함되어 있고, 서울 지역 주소지의 SH 공고가 잘 로딩되는지 확인합니다.
3. GH 청약 데이터 조회를 검증합니다.
   ```bash
   curl -s http://localhost:8080/api/subscriptions/gh/today
   ```
   **기대 결과**: 경기 4개 지역의 GH 공고가 잘 수집되어 나타나는지 확인합니다.

### B. 프론트엔드 UI 화면 검증
1. 백엔드 및 프론트엔드를 실행하고 브라우저(`http://localhost:5173/subscriptions`)에 접속합니다.
2. 청약 일정 우측 상단 탭에 **'SH 공공'** 및 **'GH 공공'** 토글 버튼이 생겼는지 확인합니다.
3. 각 탭을 클릭하여, API 키 미등록 시 "API 키가 설정되지 않았습니다. .env에 MYHOME_API_KEY를 설정하세요" 안내가 명확히 나오는지 확인하고, 등록 후에는 정상 카드 데이터가 노출되는지 체크합니다.
