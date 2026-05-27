# 1. 초기 버그 수정

커밋: `8350a8f 버그 수정: 실행 스크립트 / 고정비용 삭제 / 자산 페이지`

## 배경

`./start.sh`로 앱을 띄우려 했으나 실행 자체가 실패하거나, 자산 페이지가 흰 화면으로만 노출되고, 고정비용을 지울 때 FK 제약으로 막히는 등 기본 사용성에 문제가 있었다. 우선순위 높은 세 가지를 묶어서 처리.

## 수정 내역

### 1) `./start.sh` 부트 실패

**증상**: 백엔드 시작 시 `./gradlew: No such file or directory`

**원인**: 멀티프로젝트(`settings.gradle`에 `include 'backend'`)인데 스크립트가 `cd backend && ./gradlew bootRun`을 했다. `backend/` 하위에는 gradlew가 없다.

**수정**: 루트 gradlew로 `:backend:bootRun` 태스크 호출. 추가로 `.env` 자동 로드 블록 삽입.

```bash
cd "$PROJECT_ROOT"
./gradlew :backend:bootRun --console=plain &
```

### 2) 고정 비용(RecurringTransaction) 삭제 시 FK 위반

**증상**: Budget 페이지에서 "윈도우 할부" 고정비용 삭제 시 외래키 제약 위반

**원인**: 해당 고정비용을 참조하는 `Transaction.recurring_transaction_id` 행이 남아있는 채로 부모를 삭제 시도

**수정**: `TransactionRepository`에 JPQL UPDATE 추가 후 `RecurringTransactionService.delete()`에서 먼저 호출. 거래 이력 자체는 보존하면서 FK만 끊는다.

```java
@Modifying
@Query("UPDATE Transaction t SET t.recurringTransaction = null " +
       "WHERE t.recurringTransaction.id = :recurringTransactionId")
int detachFromRecurringTransaction(@Param("recurringTransactionId") Long id);
```

### 3) `/assets` 페이지 흰 화면

**증상**: 모든 페이지가 보이지 않음 (ReferenceError가 전역적으로 페이지 렌더링을 깸)

**원인**: `Assets.tsx`에서 `Star` 아이콘과 `formatNumber/formatExpr/evaluateExpr` 유틸을 사용하면서 import를 누락

**수정**: 누락된 import 추가

```tsx
import { Star, ... } from 'lucide-react';
import { formatCurrency, cn, formatNumber, formatExpr, evaluateExpr } from '../utils';
```

## 영향 파일

- `start.sh`
- `backend/src/main/java/org/example/account/repository/TransactionRepository.java`
- `backend/src/main/java/org/example/account/service/RecurringTransactionService.java`
- `frontend/src/pages/Assets.tsx`
