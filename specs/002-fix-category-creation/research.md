# Research: Category Serialization & Validation

본 조사 보고서에서는 카테고리 등록/수정 시 발생하는 빈 문자열 역직렬화 예외 해결책과 입력값 유효성 검증 최적화 방안을 논의한다.

## 1. Jackson Enum 역직렬화 시 빈 문자열 예외 원인 및 대안

### 문제 분석
클라이언트(프론트엔드)가 API 요청 JSON 본문에 `"yearEndCategory": ""`를 담아 전송하면, 백엔드의 Jackson `ObjectMapper`가 Enum 타입인 `YearEndCategory`로 역직렬화(Deserialization)를 시도한다. 
그러나 Jackson의 기본 바인더는 빈 문자열을 Enum 상수 중 하나로 해석할 수 없으므로 `HttpMessageNotReadableException`을 발생시킨다.

### 대안 비교

| 옵션 | 구현 방식 | 장점 | 단점 / 채택 여부 |
|------|-----------|------|-----------------|
| **A. Custom JsonDeserializer 구현** | `JsonDeserializer<YearEndCategory>` 클래스를 상속받아 커스텀 파싱 로직을 구현하고 필드에 `@JsonDeserialize` 선언 | 역직렬화 동작을 세밀하게 제어 가능 | 별도의 클래스 선언이 필요하여 코드가 복잡해짐 (미채택) |
| **B. @JsonCreator 팩토리 메서드 구현** | `YearEndCategory` Enum 내부에 `@JsonCreator` 어노테이션이 지정된 정적 팩토리 메서드를 선언하여 빈 값 처리 | 별도 클래스 없이 Enum 내부에서 깔끔하게 기본값 처리 가능 | 없음 **(최적 대안으로 채택)** |

### 채택안 상세 설계 (Option B)
`YearEndCategory.java` 내부에 Jackson 직렬화 팩토리 메서드를 추가한다.
```java
@JsonCreator
public static YearEndCategory from(String value) {
    if (value == null || value.trim().isEmpty()) {
        return NONE;
    }
    try {
        return YearEndCategory.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
        return NONE; // 정의되지 않은 값 수신 시에도 안전하게 NONE으로 복구
    }
}
```

---

## 2. 카테고리 입력값 유효성 검증 최적화

### A. 카테고리 이름 빈 값 및 공백 검증 (Trimming)
- **프론트엔드**: API 요청을 보내기 전 입력 필드의 공백을 제거(`name.trim()`)하여 비어 있는 경우 요청을 선제 차단하고 사용자 알림을 띄운다.
- **백엔드**: DTO를 서비스 레이어에서 처리하기 전 `request.name() == null || request.name().trim().isEmpty()` 조건으로 검사하여 `IllegalArgumentException`을 발생시킨다.

### B. 카테고리 이름 중복 체크
- `CategoryRepository`에 `boolean existsByName(String name)` 쿼리 메서드가 이미 존재하거나 추가하여 검증을 처리한다.
- 카테고리 등록 및 수정 시, 동일한 이름을 가진 다른 카테고리가 이미 존재하는 경우 `IllegalArgumentException`을 던져 예외를 호출자에게 전달한다.
  - *단, 수정 시에는 현재 수정 중인 카테고리 본인의 이름은 중복 체크 대상에서 제외한다.*

---

## 3. 비즈니스 규칙 제약 사항 설계 (INCOME / TRANSFER)
- 수입(`INCOME`) 및 이체(`TRANSFER`) 유형은 연말정산 신용카드 공제에 전혀 해당하지 않는다.
- 따라서, API 요청 페이로드로 `yearEndCategory`가 어떠한 값(예: `PUBLIC_TRANSPORT` 등)으로 들어오든 관계없이, 백엔드의 `CategoryService.createCategory` 및 `updateCategory` 내부에서 강제로 `YearEndCategory.NONE`으로 덮어써서 DB의 무결성을 유지하도록 처리한다.
