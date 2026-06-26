# Quickstart Guide: fix-subscription-api-keys

본 가이드는 Gradle `bootRun` 환경 하에서 `.env` 환경 변수가 자동으로 주입되어 청약일정 API 키 설정 경고 오류가 해결되었는지를 로컬에서 빠르게 확인하는 방법을 정리합니다.

## 1. 전제 조건 (Prerequisites)

1. 프로젝트 루트 폴더에 `.env` 파일이 존재해야 합니다.
2. `.env` 파일 내에 `APPLYHOME_API_KEY`와 `LH_API_KEY` 환경 변수가 올바른 인증키 값과 함께 작성되어 있어야 합니다.

```properties
# .env 예시
APPLYHOME_API_KEY=YOUR_ACTUAL_KEY
LH_API_KEY=YOUR_ACTUAL_KEY
```

---

## 2. 가동 및 검증 방법 (How to Run & Verify)

### A. 백엔드 개별 기동 테스트
1. 프로젝트 루트 폴더에서 `./start.sh` 스크립트를 거치지 않고 직접 Gradle로 백엔드를 실행합니다.
   ```bash
   ./gradlew :backend:bootRun
   ```
2. 서버 가동 완료 후 별도의 쉘 터미널에서 다음 curl 명령을 실행합니다.
   ```bash
   curl -s http://localhost:8080/api/subscriptions/today
   ```
3. **기대 결과**:
   JSON 응답에서 `"apiKeyConfigured": true`가 출력되는지 확인합니다. (이전에는 `false`가 노출되었습니다.)
   ```json
   {"asOf":"2026-06-25","apiKeyConfigured":true,"special":[],"firstRank":[],"secondRank":[],"remainder":[]}
   ```

### B. 브라우저 수동 검증
1. 백엔드 및 프론트엔드를 실행한 후 브라우저(`http://localhost:5173/subscriptions`)에 접속합니다.
2. **청약홈 (APT)** 및 **LH 공공** 탭을 클릭하여 접근합니다.
3. **기대 결과**:
   노란색의 "API 키가 설정되지 않았습니다." 경고 카드가 노출되지 않고, 정상적으로 청약 일정 데이터(존재할 경우) 혹은 빈 목록(설정 기준에 맞는 일정이 없는 경우)이 안전하게 로드되는지 확인합니다.
