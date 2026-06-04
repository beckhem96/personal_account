# Personal Account Dashboard — 작업 기록

개인 자산관리 및 세무 보조 웹 애플리케이션에 단계적으로 추가한 기능과 버그 수정 내역.
각 문서는 도입 배경, 구현 방식, 영향 받은 파일, 검증 방법을 담는다.

## 목차

| # | 문서 | 요약 |
|---|------|------|
| 1 | [초기 버그 수정](./01-initial-bugfixes.md) | `./start.sh` 실패 / 고정비용 삭제 FK 에러 / Assets 화면 흰 화면 |
| 2 | [주택 구매 비용 계산기](./02-housing-calculator.md) | 취득세·중개수수료·대출 원리금·국토부 실거래가 |
| 3 | [카드 명세서 엑셀 import](./03-card-statement-import.md) | 하나카드 명세서 → Gemini 자동 분류 → 일괄 등록 |
| 4 | [연말정산 내 거래 기반 자동 산출](./04-year-end-from-transactions.md) | 등록된 거래에서 5개 항목 자동 집계 + 카테고리 매핑 |
| 5 | [청약 일정 조회](./05-subscription-listings.md) | 청약홈 API 연동, 수도권 5개 지역의 오늘 접수중 청약 표시 |
| 6 | [LH 공공분양·임대 청약 조회](./06-lh-public-subscriptions.md) | LH 분양임대공고문 API 연동, 청약 페이지에 'LH 공공' 탭 추가 |

## 기술 스택 (전체)

- **백엔드**: Java 17, Spring Boot 3.4.1, Spring Data JPA, Gradle (멀티프로젝트)
- **DB**: MySQL 8.0 (Docker)
- **프론트엔드**: React 19, TypeScript 5.9, Vite 7, Tailwind CSS 4, Recharts
- **외부 API**: 국토교통부 실거래가, 한국부동산원 청약홈, 한국토지주택공사(LH) 분양임대공고 (공공데이터포털), Google Gemini
- **테스트**: JUnit 5 + AssertJ + Mockito

## 실행

```bash
./start.sh        # DB(Docker) + 백엔드(8080) + 프론트(5173) 동시 기동
```

`.env` 파일이 있으면 자동 로드 (API 키 등). `.env`는 `.gitignore`에 포함되어 있어 절대 커밋되지 않는다.

## 도메인 언어

UI 텍스트, 커밋 메시지, 도메인 로직 주석은 모두 한국어. 코드 식별자는 영어.
