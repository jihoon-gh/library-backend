# Coding Standards Skill

## Description
Java 25 + Spring Boot 4.0 프로젝트를 위한 코딩 표준

## When to use
- 코드 리뷰 시
- 새로운 클래스/메서드 작성 시
- 리팩토링 시
- 테스트 코드 작성 시

---

# Coding Standards - Java 25 Edition

> 원본: skills/coding-standards.md
> 
> Java 25 + Spring Boot 4.0 프로젝트를 위한 코딩 표준

## 1. 명명 규칙 (Naming Conventions)

```java
// ✅ GOOD: 서술적인 이름
public class MarketSearchService {
    
    private final String marketSearchQuery = "election";
    private final boolean isUserAuthenticated = true;
    private final BigDecimal totalRevenue = BigDecimal.valueOf(1000);
    
    public List<Market> searchMarkets(MarketSearchCriteria criteria) {
        var activeMarkets = filterActiveMarkets(criteria);
        var sortedByRelevance = sortByRelevance(activeMarkets);
        return limitResults(sortedByRelevance, criteria.maxResults());
    }
}

// ❌ BAD: 불명확한 이름
public class Svc {
    private String q = "election";      // 무엇의 query인가?
    private boolean flag = true;         // 어떤 상태를 나타내는가?
    private int x = 1000;               // 무슨 값인가?
    
    public List<Object> proc(Object c) { // proc? c?
        return null;
    }
}
```

### 명명 규칙 체크리스트

- [ ] 클래스명은 **명사** 또는 **명사구** (예: `MarketService`, `OrderProcessor`)
- [ ] 메서드명은 **동사** 또는 **동사구** (예: `findById`, `calculateTotal`)
- [ ] boolean은 **is/has/can/should** 접두사 (예: `isActive`, `hasPermission`)
- [ ] 상수는 **UPPER_SNAKE_CASE** (예: `MAX_RETRY_ATTEMPTS`)
- [ ] 패키지명은 **소문자** (예: `com.example.market.domain`)

---

## 2. 함수 크기 제한 (Martin Fowler - Extract Method)

```java
// ❌ BAD: 50줄 이상의 함수 (God Method)
public class MarketProcessor {
    
    public void processMarketData(RawMarketData data) {
        // 100줄의 코드...
        // 검증, 변환, 저장, 알림 모두 한 곳에
    }
}

// ✅ GOOD: 작은 함수로 분리 (Single Responsibility)
public class MarketProcessor {
    
    private final MarketValidator validator;
    private final MarketTransformer transformer;
    private final MarketRepository repository;
    
    public Market processMarketData(RawMarketData data) {
        var validated = validator.validate(data);
        var transformed = transformer.transform(validated);
        return repository.save(transformed);
    }
}

// ✅ 각 책임을 별도 클래스로 분리 (SRP)
public class MarketValidator {
    
    public ValidatedMarketData validate(RawMarketData data) {
        ensureNotNull(data);
        ensureValidFormat(data);
        ensureBusinessRulesPass(data);
        return new ValidatedMarketData(data);
    }
    
    private void ensureNotNull(RawMarketData data) {
        if (data == null) {
            throw new MarketValidationException("Market data cannot be null");
        }
    }
}
```

### 크기 제한 기준

| 항목 | 최대값 | 초과 시 조치 |
|------|--------|--------------|
| 클래스 | 400줄 | 책임 분리 |
| 메서드 | 50줄 | Extract Method |
| 파라미터 | 4개 | Parameter Object |
| 중첩 깊이 | 3단계 | Early Return |

---

## 3. 중첩 깊이 제한 (Guard Clauses)

```java
// ❌ BAD: 5단계 이상의 중첩 (Arrow Anti-Pattern)
public class AuthorizationService {
    
    public void performAction(User user, Market market, Permission permission) {
        if (user != null) {
            if (user.isAdmin()) {
                if (market != null) {
                    if (market.isActive()) {
                        if (hasPermission(user, permission)) {
                            executeAction(user, market);
                        }
                    }
                }
            }
        }
    }
}

// ✅ GOOD: Early Return으로 평탄화 (Guard Clauses)
public class AuthorizationService {
    
    public void performAction(User user, Market market, Permission permission) {
        validatePreconditions(user, market, permission);
        executeAction(user, market);
    }
    
    private void validatePreconditions(User user, Market market, Permission permission) {
        if (user == null) {
            throw new UnauthorizedException("User is required");
        }
        if (!user.isAdmin()) {
            throw new ForbiddenException("Admin access required");
        }
        if (market == null) {
            throw new NotFoundException("Market not found");
        }
        if (!market.isActive()) {
            throw new BusinessException("Market is not active");
        }
        if (!hasPermission(user, permission)) {
            throw new ForbiddenException("Insufficient permissions");
        }
    }
}
```

---

## 4. 매직 넘버 제거 (No Magic Numbers)

```java
// ❌ BAD: 설명 없는 숫자
public class RetryService {
    
    public void executeWithRetry(Runnable action) {
        int attempts = 0;
        while (attempts < 3) {  // 3이 뭔가?
            try {
                action.run();
                return;
            } catch (Exception e) {
                attempts++;
                sleep(500);  // 500이 뭔가?
            }
        }
    }
}

// ✅ GOOD: 의미 있는 상수 정의
public class RetryService {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);
    
    public void executeWithRetry(Runnable action) {
        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                action.run();
                return;
            } catch (Exception e) {
                attempts++;
                sleep(RETRY_DELAY);
            }
        }
        throw new RetryExhaustedException(
            "Failed after %d attempts".formatted(MAX_RETRY_ATTEMPTS)
        );
    }
}

// ✅ BETTER: 설정으로 추출 (Open-Closed Principle)
@ConfigurationProperties(prefix = "retry")
public record RetryProperties(
    int maxAttempts,
    Duration delay,
    double backoffMultiplier
) {
    public RetryProperties {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative");
        }
    }
}
```

---

## 5. 테스트 명명 규칙 (한글 권장)

```java
// ✅ GOOD: 서술적인 테스트 이름
@DisplayName("MarketSearchService")
class MarketSearchServiceTest {
    
    @Test
    @DisplayName("검색어와 일치하는 마켓이 없으면 빈 리스트를 반환한다")
    void returns_empty_list_when_no_markets_match_query() {
        // given
        var service = new MarketSearchService(emptyRepository());
        var criteria = MarketSearchCriteria.of("nonexistent");
        
        // when
        var results = service.search(criteria);
        
        // then
        assertThat(results).isEmpty();
    }
    
    @Test
    @DisplayName("Redis를 사용할 수 없으면 문자열 검색으로 폴백한다")
    void falls_back_to_substring_search_when_redis_unavailable() {
        // given
        var service = new MarketSearchService(unavailableRedis(), fallbackSearchEngine());
        
        // when
        var results = service.search(MarketSearchCriteria.of("election"));
        
        // then
        assertThat(results).isNotEmpty();
        verify(fallbackSearchEngine()).search(any());
    }
}

// ❌ BAD: 모호한 테스트 이름
class MarketTest {
    @Test void works() { }           // 뭐가 works?
    @Test void testSearch() { }      // test 접두사 불필요
    @Test void test1() { }           // 의미 없음
}
```

---

## 6. 주석 작성 원칙 (WHY, not WHAT)

```java
// ✅ GOOD: WHY를 설명
public class MarketPriceService {
    
    /**
     * 지수 백오프를 사용하여 API 장애 시 서버 과부하를 방지합니다.
     * 최대 30초까지 대기하며, 이는 API 제공자의 권장 사항입니다.
     */
    private Duration calculateBackoffDelay(int retryCount) {
        var exponentialDelay = Duration.ofSeconds((long) Math.pow(2, retryCount));
        var maxDelay = Duration.ofSeconds(30);
        return exponentialDelay.compareTo(maxDelay) < 0 ? exponentialDelay : maxDelay;
    }
}

// ❌ BAD: WHAT을 설명 (코드가 이미 말하고 있음)
// 카운터를 1 증가시킴
count++;

// user를 null로 설정
user = null;

// 리스트에서 첫 번째 요소를 가져옴
var first = list.get(0);
```

---

## 체크리스트

코드 작성 시 확인하세요:

- [ ] 모든 이름이 의도를 명확히 드러내는가?
- [ ] 메서드가 50줄을 넘지 않는가?
- [ ] 중첩 깊이가 3단계 이하인가?
- [ ] 매직 넘버가 상수로 추출되었는가?
- [ ] 테스트 이름이 동작을 설명하는가?
- [ ] 주석이 WHY를 설명하는가?

---

**Remember: 코드 품질은 협상 대상이 아닙니다.**
