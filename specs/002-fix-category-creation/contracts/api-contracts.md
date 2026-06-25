# API Contracts: Category Management

본 문서에서는 카테고리 생성 및 수정을 처리하는 REST API 계약을 명세한다.

## 1. 카테고리 생성 (Create Category)

*   **URL**: `/api/categories`
*   **Method**: `POST`
*   **Headers**: `Content-Type: application/json`

### Request Payload Schema
```json
{
  "name": "식비",
  "type": "EXPENSE",
  "yearEndCategory": "NONE"
}
```

| 필드명 | 타입 | 필수 여부 | 검증 및 변환 규칙 |
|--------|------|-----------|------------------|
| **`name`** | String | 필수 | 앞뒤 공백 제거(Trim) 후 최소 1자 이상이어야 함. 중복 불가. |
| **`type`** | String | 필수 | `"EXPENSE"`, `"INCOME"`, `"TRANSFER"` 중 하나여야 함. |
| **`yearEndCategory`** | String | 선택 | `"NONE"`, `"TRADITIONAL_MARKET"`, `"PUBLIC_TRANSPORT"` 중 하나. 빈 문자열(`""`) 혹은 `null` 전송 시 백엔드에서 자동 `"NONE"`으로 매핑됨. |

### Response Example (200 OK)
```json
{
  "id": 13,
  "name": "식비",
  "type": "EXPENSE",
  "yearEndCategory": "NONE"
}
```

### Error Responses
*   **400 Bad Request** (이름이 공백이거나 누락된 경우):
    `"카테고리 이름을 입력해 주세요."`
*   **400 Bad Request** (이름이 중복되는 경우):
    `"이미 존재하는 카테고리 이름입니다."`

---

## 2. 카테고리 수정 (Update Category)

*   **URL**: `/api/categories/{id}`
*   **Method**: `PUT`
*   **Headers**: `Content-Type: application/json`

### Request Payload Schema
(생성 요청 스키마와 동일)

### Response Example (200 OK)
```json
{
  "id": 13,
  "name": "새로운 식비",
  "type": "EXPENSE",
  "yearEndCategory": "TRADITIONAL_MARKET"
}
```

### Error Responses
*   **400 Bad Request**: 이름 누락, 공백 혹은 중복 오류 발생 시 생성 API와 동일한 포맷의 예외 반환.
*   **404 Not Found**: 존재하지 않는 `{id}`를 수정하려 시도하는 경우.
