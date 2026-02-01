# Testing Rules - Java 25 Edition

> 원본: rules/testing.md
> 
> 프로젝트 전체에 적용되는 테스팅 규칙

---

## 규칙 1: TDD 워크플로우를 따른다

```
┌─────────┐     ┌─────────┐     ┌──────────┐
│   RED   │ ──▶ │  GREEN  │ ──▶ │ REFACTOR │
│실패 테스트│     │최소 구현 │     │설계 개선  │
└─────────┘     └─────────┘     └──────────┘
     ▲                               │
     └───────────────────────────────┘
```

1. **RED**: 실패하는 테스트 작성
2. **GREEN**: 테스트를 통과하는 최소한의 코드
3. **REFACTOR**: 중복 제거 및 설계 개선

**"테스트 없는 코드는 레거시다" - Michael Feathers**

---

## 규칙 2: 80% 이상의 테스트 커버리지 유지

### Gradle 설정

```kotlin
// build.gradle.kts
plugins {
    id("jacoco")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = BigDecimal("0.80")  // 80% 최소
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

## 규칙 3: FIRST 원칙을 따른다

| 원칙 | 설명 |
|------|------|
| **F**ast | 테스트는 빨라야 한다 |
| **I**ndependent | 테스트 간 의존성 없음 |
| **R**epeatable | 어디서든 같은 결과 |
| **S**elf-validating | 성공/실패 명확 |
| **T**imely | 프로덕션 코드 직전에 작성 |

---

## 규칙 4: Given-When-Then 패턴을 사용한다

```java
@Test
@DisplayName("활성 사용자가 마켓을 생성하면 마켓이 저장된다")
void active_user_creates_market_then_market_is_saved() {
    // given - 사전 조건 설정
    var user = UserFixture.activeUser();
    var command = new CreateMarketCommand("Test Market", "Description");
    
    // when - 테스트 대상 행동 실행
    var result = marketService.createMarket(user, command);
    
    // then - 결과 검증
    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("Test Market");
    verify(marketRepository).save(any(Market.class));
}
```

---

## 규칙 5: 테스트 이름은 동작을 설명한다

### ✅ 좋은 테스트 이름 (한글 권장)

```java
@DisplayName("MarketService")
class MarketServiceTest {
    
    @Test
    @DisplayName("마켓 ID로 조회하면 해당 마켓을 반환한다")
    void returns_market_when_found_by_id() { }
    
    @Test
    @DisplayName("존재하지 않는 마켓 ID로 조회하면 빈 Optional을 반환한다")
    void returns_empty_optional_when_market_not_found() { }
    
    @Test
    @DisplayName("마켓 생성 시 MarketCreatedEvent가 발행된다")
    void publishes_market_created_event_when_market_is_created() { }
    
    @Test
    @DisplayName("마감된 마켓에 베팅하면 MarketClosedException이 발생한다")
    void throws_market_closed_exception_when_betting_on_closed_market() { }
}
```

### ❌ 나쁜 테스트 이름

```java
@Test void test1() { }
@Test void testCreate() { }
@Test void works() { }
@Test void shouldWork() { }
```

---

## 규칙 6: 테스트 더블을 적절히 사용한다

### Stub - 미리 정해진 값 반환

```java
@Test
@DisplayName("마켓이 존재하면 마켓 정보를 반환한다")
void returns_market_info_when_market_exists() {
    // given
    var marketId = MarketId.of(UUID.randomUUID());
    var market = MarketFixture.active();
    
    // Stub: 미리 정해진 값 반환
    when(marketRepository.findById(marketId))
        .thenReturn(Optional.of(market));
    
    // when
    var result = marketService.findById(marketId);
    
    // then
    assertThat(result).contains(market);
}
```

### Mock - 호출 검증

```java
@Test
@DisplayName("마켓 생성 시 이벤트가 발행된다")
void publishes_event_when_market_is_created() {
    // given
    var command = new CreateMarketCommand("Test", "Description");
    var eventPublisher = mock(EventPublisher.class);
    var service = new MarketService(marketRepository, eventPublisher);
    
    // when
    service.createMarket(command);
    
    // then - Mock: 호출 검증
    verify(eventPublisher).publish(any(MarketCreatedEvent.class));
    verify(eventPublisher, times(1)).publish(any());
    verify(eventPublisher, never()).publish(any(MarketClosedEvent.class));
}
```

### Fake - 실제 동작하는 가짜 구현

```java
// 테스트용 인메모리 저장소
public class InMemoryMarketRepository implements MarketRepository {
    
    private final Map<MarketId, Market> storage = new ConcurrentHashMap<>();
    
    @Override
    public Market save(Market market) {
        storage.put(market.id(), market);
        return market;
    }
    
    @Override
    public Optional<Market> findById(MarketId id) {
        return Optional.ofNullable(storage.get(id));
    }
    
    @Override
    public List<Market> findAll() {
        return List.copyOf(storage.values());
    }
    
    @Override
    public void delete(MarketId id) {
        storage.remove(id);
    }
    
    // 테스트 헬퍼 메서드
    public void clear() {
        storage.clear();
    }
    
    public int count() {
        return storage.size();
    }
}

// 사용 예시
@Test
void integration_test_with_fake_repository() {
    // given
    var repository = new InMemoryMarketRepository();
    var service = new MarketService(repository, eventPublisher);
    
    // when
    service.createMarket(new CreateMarketCommand("Test", "Desc"));
    
    // then
    assertThat(repository.count()).isEqualTo(1);
}
```

---

## 규칙 7: Fixture를 사용한다

```java
public class MarketFixture {
    
    public static Market active() {
        return Market.create(
            "Test Market",
            "Test Description",
            Instant.now().plus(Duration.ofDays(7))
        );
    }
    
    public static Market closed() {
        var market = active();
        market.close(Resolution.YES);
        return market;
    }
    
    public static Market withTitle(String title) {
        return Market.create(
            title,
            "Test Description",
            Instant.now().plus(Duration.ofDays(7))
        );
    }
    
    public static List<Market> listOf(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> withTitle("Market " + i))
            .toList();
    }
}

public class UserFixture {
    
    public static User activeUser() {
        return new User(
            UserId.generate(),
            "test@example.com",
            "Test User",
            UserStatus.ACTIVE
        );
    }
    
    public static User admin() {
        return new User(
            UserId.generate(),
            "admin@example.com",
            "Admin User",
            UserStatus.ACTIVE,
            Set.of(Role.ADMIN)
        );
    }
}
```

---

## 규칙 8: 테스트를 구조화한다

```java
@DisplayName("MarketService")
class MarketServiceTest {
    
    @Nested
    @DisplayName("findById 메서드는")
    class FindById {
        
        @Test
        @DisplayName("존재하는 ID로 조회하면 마켓을 반환한다")
        void returns_market_when_exists() { }
        
        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
        void returns_empty_when_not_exists() { }
    }
    
    @Nested
    @DisplayName("createMarket 메서드는")
    class CreateMarket {
        
        @Test
        @DisplayName("유효한 명령으로 마켓을 생성한다")
        void creates_market_with_valid_command() { }
        
        @Test
        @DisplayName("중복 제목이면 DuplicateTitleException을 던진다")
        void throws_exception_when_title_duplicated() { }
        
        @Test
        @DisplayName("마켓 생성 시 이벤트를 발행한다")
        void publishes_event_when_created() { }
    }
    
    @Nested
    @DisplayName("closeMarket 메서드는")
    class CloseMarket {
        
        @Test
        @DisplayName("활성 마켓을 종료한다")
        void closes_active_market() { }
        
        @Test
        @DisplayName("이미 종료된 마켓이면 예외를 던진다")
        void throws_exception_when_already_closed() { }
    }
}
```

---

## 테스팅 체크리스트

테스트 작성 시 확인하세요:

- [ ] 테스트가 먼저 작성되었는가? (TDD)
- [ ] Given-When-Then 패턴을 따르는가?
- [ ] 테스트 이름이 동작을 설명하는가?
- [ ] 테스트가 독립적인가?
- [ ] 테스트가 빠른가?
- [ ] 테스트 더블이 적절히 사용되었는가?
- [ ] Fixture가 재사용되는가?
- [ ] 커버리지가 80% 이상인가?

---

**Remember: 테스트는 코드의 첫 번째 사용자입니다.**
