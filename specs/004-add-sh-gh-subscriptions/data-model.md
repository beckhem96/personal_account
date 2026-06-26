# Data Model & Configuration: add-sh-gh-subscriptions

본 기능 구현을 위한 환경 변수 정의 및 국토교통부 마이홈 API 응답 필드와 로컬 DTO 간의 매핑 명세입니다.

## 1. 환경 변수 구성 (Environment Variables)

| 설정 항목 (YML Path) | 환경 변수 Key (OS Env) | 용도 |
|-------------------|----------------------|-----|
| `myhome.api-key` | `MYHOME_API_KEY` | 국토교통부 마이홈포털 오픈 API 인증키 |

## 2. API 응답 필드 매핑 규칙 (Field Mapping)

마이홈포털 API의 XML/JSON 응답 요소에서 아래 규칙에 따라 공통 DTO 속성을 매핑합니다.

| 마이홈 API 원본 필드명 | 로컬 DTO 속성명 | 매핑 규칙 및 설명 |
|---------------------|---------------|-----------------|
| `pblancNm` (공고명) | `name` | 주택명 또는 공고 제목 |
| `hssplyAdres` (주소) | `address` | 주택의 실제 소재지 지번/도로명 주소 |
| `rcritNtcDe` (공고일) | `noticeDate` | 최초 모집공고 게시일 (`YYYY-MM-DD`로 포맷 보정) |
| `rceptBgnde` (접수시작일) | `rcptBegin` | 청약 신청 접수 시작일 (`YYYY-MM-DD`로 포맷 보정) |
| `rceptEndde` (접수종료일) | `rcptEnd` | 청약 신청 접수 마감일 (`YYYY-MM-DD`로 포맷 보정) |
| `pblancUrl` (상세URL) | `detailUrl` | 마이홈포털 공고 세부 페이지 이동용 하이퍼링크 |
| `suplyInsttNm` (기관명) | `supplyTypeName` | "서울주택도시공사" 또는 "경기주택도시공사" 분류 판정 및 공급유형 레이블 |

## 3. 백엔드 DTO 재사용
- 기존 LH 공고용 DTO인 `LhNoticesResponse` 와 `LhNoticeItem` 클래스를 그대로 재사용하거나, 명확성을 위해 `MyHomeNoticeItem` 등으로 설계할 수 있으나, 프론트엔드 호환성을 고려해 기존 LH 카드 렌더링에 적합한 데이터 구조로 변환하여 동일 포맷으로 전달하는 방향을 지향합니다.
