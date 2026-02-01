# TDD Workflow - Java 25 Edition

> 원본: skills/tdd-workflow/SKILL.md
> 
> Kent Beck의 TDD 워크플로우를 Java 25로 구현

## 언제 사용하나요?

- 새로운 기능을 개발할 때
- 버그를 수정할 때
- 코드를 리팩터링할 때

**항상 테스트를 먼저 작성하고, 그 다음에 코드를 작성합니다.**

---

## TDD 사이클

```
┌─────────┐     ┌─────────┐     ┌──────────┐
│   RED   │ ──▶ │  GREEN  │ ──▶ │ REFACTOR │
│실패 테스트│     │최소 구현 │     │설계 개선  │
└─────────┘     └─────────┘     └──────────┘
     ▲                               │
     └───────────────────────────────┘
```

### TDD Phase를 타입으로 표현

```java
/**
 * TDD 워크플로우를 타입 시스템으로 표현합니다.
 * 
 * Kent Beck의 TDD 사이클:
 * 1. RED: 실패하는 테스트 작성
 * 2. GREEN: 테스트를 통과하는 최소한의 코드 작성
 * 3. REFACTOR: 중복 제거, 설계 개선
 */
public sealed interface TddPhase permits TddPhase.Red, TddPhase.Green, TddPhase.Refactor {
    
    /**
     * RED 단계: 실패하는 테스트를 작성합니다.
     * 
     * 이 단계에서는:
     * - 원하는 동작을 테스트로 명세화
     * - 테스트가 실패하는지 확인 (컴파일 에러 포함)
     * - 테스트가 올바른 이유로 실패하는지 확인
     */
    record Red(TestSpecification spec) implements TddPhase {
        public Green implement(Implementation impl) {
            return new Green(spec, impl);
        }
    }
    
    /**
     * GREEN 단계: 테스트를 통과하는 최소한의 코드를 작성합니다.
     * 
     * 이 단계에서는:
     * - 가장 단순한 구현으로 시작
     * - "죄악"을 저질러도 됨 (하드코딩, 중복 등)
     * - 목표는 오직 "테스트 통과"
     */
    record Green(TestSpecification spec, Implementation impl) implements TddPhase {
        public Refactor refactor(RefactoringPlan plan) {
            return new Refactor(spec, impl, plan);
        }
    }
    
    /**
     * REFACTOR 단계: 동작을 유지하면서 설계를 개선합니다.
     * 
     * 이 단계에서는:
     * - 중복 제거 (DRY)
     * - 명확한 이름 사용
     * - 작은 메서드로 추출
     * - 테스트가 계속 통과하는지 확인
     */
    record Refactor(TestSpecification spec, Implementation impl, RefactoringPlan plan) implements TddPhase {
        public Red nextCycle(TestSpecification nextSpec) {
            return new Red(nextSpec);
        }
    }
}
```

---

## TDD 실습 예제: Money 클래스

> Kent Beck의 "Test Driven Development: By Example"에서 영감을 받음

### Step 1: RED - 실패하는 테스트

```java
@DisplayName("Money")
class MoneyTest {
    
    @Test
    @DisplayName("5달러에 5달러를 더하면 10달러가 된다")
    void five_dollars_plus_five_dollars_equals_ten_dollars() {
        // given
        var five = Money.dollar(5);
        
        // when
        var result = five.plus(Money.dollar(5));
        
        // then
        assertThat(result).isEqualTo(Money.dollar(10));
    }
    
    @Test
    @DisplayName("5달러에 2를 곱하면 10달러가 된다")
    void five_dollars_times_two_equals_ten_dollars() {
        // given
        var five = Money.dollar(5);
        
        // when
        var result = five.times(2);
        
        // then
        assertThat(result).isEqualTo(Money.dollar(10));
    }
    
    @Test
    @DisplayName("동일한 금액의 달러는 같다")
    void dollars_with_same_amount_are_equal() {
        assertThat(Money.dollar(5)).isEqualTo(Money.dollar(5));
        assertThat(Money.dollar(5)).isNotEqualTo(Money.dollar(6));
    }
    
    @Test
    @DisplayName("달러와 원은 다르다")
    void dollar_is_not_equal_to_won() {
        assertThat(Money.dollar(5)).isNotEqualTo(Money.won(5));
    }
}
```

### Step 2: GREEN - 최소한의 구현

```java
/**
 * 금액을 나타내는 불변 값 객체.
 * 
 * 값 객체의 특성:
 * - 불변 (Immutable)
 * - 속성으로 동등성 비교 (equals/hashCode)
 * - 부작용 없음 (Side-effect free)
 */
public record Money(int amount, Currency currency) {
    
    // Compact Constructor로 유효성 검증
    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
        Objects.requireNonNull(currency, "Currency is required");
    }
    
    // 팩토리 메서드
    public static Money dollar(int amount) {
        return new Money(amount, Currency.USD);
    }
    
    public static Money won(int amount) {
        return new Money(amount, Currency.KRW);
    }
    
    // 연산 (항상 새 객체 반환)
    public Money plus(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount + other.amount, this.currency);
    }
    
    public Money times(int multiplier) {
        return new Money(this.amount * multiplier, this.currency);
    }
    
    private void ensureSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(
                "Cannot add %s to %s".formatted(other.currency, this.currency)
            );
        }
    }
}

enum Currency {
    USD("$"), 
    KRW("₩"), 
    EUR("€");
    
    private final String symbol;
    
    Currency(String symbol) {
        this.symbol = symbol;
    }
    
    public String symbol() { return symbol; }
}
```

### Step 3: REFACTOR - 환율 변환 추가

```java
// 추가 테스트
@Test
@DisplayName("5달러를 원화로 환전하면 6,500원이 된다")
void five_dollars_converts_to_six_thousand_five_hundred_won() {
    // given
    var fiveDollars = Money.dollar(5);
    var rateProvider = mock(ExchangeRateProvider.class);
    when(rateProvider.getRate(Currency.USD, Currency.KRW))
        .thenReturn(BigDecimal.valueOf(1300));
    
    // when
    var result = fiveDollars.convertTo(Currency.KRW, rateProvider);
    
    // then
    assertThat(result).isEqualTo(Money.won(6500));
}

// 리팩터링된 Money
public record Money(BigDecimal amount, Currency currency) {
    
    public Money {
        Objects.requireNonNull(amount, "Amount is required");
        Objects.requireNonNull(currency, "Currency is required");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public static Money dollar(int amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.USD);
    }
    
    /**
     * 다른 통화로 환전합니다.
     */
    public Money convertTo(Currency target, ExchangeRateProvider rateProvider) {
        if (this.currency.equals(target)) {
            return this;
        }
        
        var rate = rateProvider.getRate(this.currency, target);
        var converted = this.amount.multiply(rate)
            .setScale(0, RoundingMode.HALF_UP);
        
        return new Money(converted, target);
    }
}

// 환율 제공자 인터페이스 (의존성 역전)
public interface ExchangeRateProvider {
    BigDecimal getRate(Currency from, Currency to);
}
```

---

## TDD 체크리스트

### RED 단계

- [ ] 테스트가 컴파일되는가?
- [ ] 테스트가 실패하는가?
- [ ] 올바른 이유로 실패하는가?
- [ ] 테스트 이름이 동작을 설명하는가?
- [ ] 테스트당 하나의 검증만 있는가?

### GREEN 단계

- [ ] 테스트가 통과하는가?
- [ ] 가장 단순한 구현인가?
- [ ] 성급한 최적화를 하지 않았는가?
- [ ] 기존 테스트도 모두 통과하는가?

### REFACTOR 단계

- [ ] 중복이 제거되었는가?
- [ ] 이름이 명확한가?
- [ ] 메서드가 작은가?
- [ ] 테스트가 여전히 통과하는가?
- [ ] 새로운 동작을 추가하지 않았는가?

---

## 커버리지 요구사항

```kotlin
// build.gradle.kts
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = BigDecimal("0.80")  // 80% 최소 커버리지
            }
        }
        rule {
            element = "CLASS"
            excludes = listOf(
                "*.config.*",
                "*.dto.*",
                "*Application"
            )
            limit {
                counter = "LINE"
                minimum = BigDecimal("0.80")
            }
        }
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    finalizedBy(tasks.jacocoTestCoverageVerification)
}
```

---

**Remember: 테스트 없는 코드는 레거시다. - Michael Feathers**
