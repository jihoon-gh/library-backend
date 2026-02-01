# Coding Style Rules - Java 25 Edition

> 원본: rules/coding-style.md
> 
> 프로젝트 전체에 적용되는 코딩 스타일 규칙

---

## 규칙 1: 불변 객체를 우선한다

### ❌ 가변 객체

```java
public class MutablePerson {
    private String name;
    private int age;
    
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
}
```

### ✅ 불변 객체 (Java 25 record)

```java
public record Person(String name, int age) {
    
    // Compact Constructor로 검증
    public Person {
        Objects.requireNonNull(name, "name is required");
        if (age < 0) {
            throw new IllegalArgumentException("age must be non-negative");
        }
    }
    
    // 변경이 필요하면 새 객체 반환 (with 패턴)
    public Person withName(String newName) {
        return new Person(newName, this.age);
    }
    
    public Person withAge(int newAge) {
        return new Person(this.name, newAge);
    }
}
```

### ✅ 불변 컬렉션

```java
// 불변 리스트
private final List<String> items = List.of("a", "b", "c");

// 불변 맵
private final Map<String, Integer> scores = Map.of(
    "alice", 100,
    "bob", 90
);

// 불변 Set
private final Set<String> tags = Set.of("java", "spring");

// 외부로 반환할 때도 불변으로
public List<String> getItems() {
    return List.copyOf(items);  // 또는 Collections.unmodifiableList(items)
}
```

---

## 규칙 2: 파일 크기를 제한한다

| 항목 | 최대값 | 초과 시 조치 |
|------|--------|--------------|
| 클래스 | 400줄 | 책임 분리 |
| 메서드 | 50줄 | Extract Method |
| 파라미터 | 4개 | Parameter Object |
| 중첩 깊이 | 3단계 | Early Return |

### 파라미터가 많을 때

```java
// ❌ 파라미터 5개 이상
public void createOrder(
    String customerId,
    String productId,
    int quantity,
    String shippingAddress,
    String billingAddress,
    String couponCode
) { }

// ✅ Parameter Object 사용
public record CreateOrderCommand(
    String customerId,
    String productId,
    int quantity,
    String shippingAddress,
    String billingAddress,
    @Nullable String couponCode
) {
    public CreateOrderCommand {
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(productId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}

public void createOrder(CreateOrderCommand command) { }
```

---

## 규칙 3: 예외를 적절히 처리한다

### ❌ 예외 무시

```java
try {
    doSomething();
} catch (Exception e) {
    // 무시 - 절대 금지!
}
```

### ❌ 모든 예외를 잡음

```java
try {
    doSomething();
} catch (Exception e) {  // 너무 광범위
    log.error("Error", e);
}
```

### ✅ 구체적인 예외 처리

```java
public Optional<User> findUser(String id) {
    try {
        return Optional.of(userRepository.findById(id));
    } catch (UserNotFoundException e) {
        log.debug("User not found: {}", id);
        return Optional.empty();
    } catch (DatabaseException e) {
        log.error("Database error while finding user: {}", id, e);
        throw new ServiceException("Failed to find user", e);
    }
}
```

### ✅ 커스텀 도메인 예외 (sealed class)

```java
// 기본 도메인 예외
public sealed class DomainException extends RuntimeException 
    permits MarketNotFoundException, InvalidOperationException, InsufficientFundsException {
    
    public DomainException(String message) {
        super(message);
    }
    
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

// 구체적인 예외들
public final class MarketNotFoundException extends DomainException {
    public MarketNotFoundException(MarketId id) {
        super("Market not found: " + id);
    }
}

public final class InvalidOperationException extends DomainException {
    public InvalidOperationException(String operation, String reason) {
        super("Cannot %s: %s".formatted(operation, reason));
    }
}

public final class InsufficientFundsException extends DomainException {
    public InsufficientFundsException(Money required, Money available) {
        super("Insufficient funds: required %s, available %s".formatted(required, available));
    }
}
```

---

## 규칙 4: Null을 피한다

### ❌ null 반환

```java
public User findById(String id) {
    var user = repository.findById(id);
    return user;  // null일 수 있음!
}
```

### ✅ Optional 사용

```java
public Optional<User> findById(String id) {
    return repository.findById(id);
}

// 호출하는 쪽
public void processUser(String id) {
    findById(id)
        .ifPresentOrElse(
            this::process,
            () -> log.warn("User not found: {}", id)
        );
}

// 또는
public User getOrThrow(String id) {
    return findById(id)
        .orElseThrow(() -> new UserNotFoundException(id));
}

// 기본값 제공
public User getOrDefault(String id) {
    return findById(id)
        .orElse(User.anonymous());
}
```

### ✅ JSpecify 어노테이션 (Spring Boot 4.0 표준)

```java
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class UserService {
    
    // 반환값과 파라미터의 null 가능성을 명시
    public @NonNull User createUser(
        @NonNull String name, 
        @Nullable String nickname
    ) {
        var effectiveNickname = nickname != null ? nickname : name;
        return new User(name, effectiveNickname);
    }
    
    // Optional과 함께 사용
    public @NonNull Optional<User> findById(@NonNull String id) {
        return repository.findById(id);
    }
}
```

---

## 규칙 5: 로깅을 적절히 한다

### ❌ System.out 사용

```java
System.out.println("Processing order: " + orderId);
```

### ✅ SLF4J Logger 사용

```java
@Slf4j  // Lombok
public class OrderService {
    
    public void processOrder(Order order) {
        log.info("Processing order: {}", order.id());
        
        try {
            // 처리 로직
            log.debug("Order details: {}", order);
        } catch (PaymentException e) {
            log.error("Payment failed for order {}: {}", order.id(), e.getMessage(), e);
            throw e;
        }
    }
}

// Lombok 없이
public class OrderService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    // ...
}
```

### 로깅 레벨 가이드

| 레벨 | 용도 | 예시 |
|------|------|------|
| ERROR | 즉시 조치 필요한 오류 | 결제 실패, DB 연결 실패 |
| WARN | 잠재적 문제 | 재시도 발생, 캐시 미스 |
| INFO | 중요한 비즈니스 이벤트 | 주문 생성, 사용자 로그인 |
| DEBUG | 개발/디버깅용 상세 정보 | 메서드 파라미터, 중간 결과 |
| TRACE | 매우 상세한 정보 | 루프 내부, 모든 호출 |

---

## 규칙 6: 명확한 이름을 사용한다

### 클래스명

```java
// ❌ BAD
class Mgr { }
class Data { }
class Info { }

// ✅ GOOD
class OrderManager { }
class CustomerData { }
class ProductInfo { }
```

### 메서드명

```java
// ❌ BAD
void proc() { }
void handle() { }
void do() { }

// ✅ GOOD
void processOrder() { }
void handlePaymentFailure() { }
void calculateTotalPrice() { }
```

### Boolean 변수/메서드

```java
// ❌ BAD
boolean flag;
boolean check();

// ✅ GOOD
boolean isActive;
boolean hasPermission;
boolean canExecute();
boolean shouldRetry();
```

---

## 코딩 스타일 체크리스트

코드 작성 시 확인하세요:

- [ ] DTO와 값 객체가 record로 정의되어 있는가?
- [ ] 컬렉션이 불변으로 생성되었는가?
- [ ] 클래스가 400줄을 넘지 않는가?
- [ ] 메서드가 50줄을 넘지 않는가?
- [ ] 파라미터가 4개를 넘지 않는가?
- [ ] 예외가 구체적으로 처리되는가?
- [ ] null 대신 Optional을 사용하는가?
- [ ] System.out 대신 Logger를 사용하는가?
- [ ] 이름이 의도를 명확히 드러내는가?

---

**Remember: 코드는 작성하는 시간보다 읽는 시간이 더 깁니다.**
