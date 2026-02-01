# Backend Patterns Skill

## Description
Java 25 + Spring Boot 4.0 기반 API, 데이터베이스, 캐싱 패턴 가이드

## When to use
- Repository 패턴 구현 시
- 캐싱 전략 설계 시
- API Response 타입 설계 시
- HTTP 클라이언트 구현 시
- 트랜잭션 관리 시

---

# Backend Patterns - Java 25 Edition

> 원본: skills/backend-patterns.md
> 
> Java 25 + Spring Boot 4.0 기반 API, 데이터베이스, 캐싱 패턴

## 1. Repository 패턴 (DDD)

### 도메인 계층: 인터페이스 정의

```java
package com.example.market.domain.repository;

import org.jspecify.annotations.NonNull;
import java.util.List;
import java.util.Optional;

/**
 * Market 애그리거트의 저장소 인터페이스.
 * 
 * 도메인 계층에 위치하며, 구현체는 인프라스트럭처 계층에 있습니다.
 * 이를 통해 도메인이 영속화 기술에 의존하지 않습니다. (DIP)
 */
public interface MarketRepository {
    
    @NonNull Optional<Market> findById(@NonNull MarketId id);
    
    @NonNull Optional<Market> findBySlug(@NonNull String slug);
    
    @NonNull List<Market> findByStatus(@NonNull MarketStatus status);
    
    @NonNull Market save(@NonNull Market market);
    
    void delete(@NonNull MarketId id);
    
    boolean existsById(@NonNull MarketId id);
}
```

### 인프라스트럭처 계층: JPA 구현

```java
package com.example.market.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
class JpaMarketRepository implements MarketRepository {
    
    private final MarketJpaRepository jpaRepository;
    private final MarketMapper mapper;
    
    JpaMarketRepository(MarketJpaRepository jpaRepository, MarketMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public @NonNull Optional<Market> findById(@NonNull MarketId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain);
    }
    
    @Override
    public @NonNull Optional<Market> findBySlug(@NonNull String slug) {
        return jpaRepository.findBySlug(slug)
            .map(mapper::toDomain);
    }
    
    @Override
    @Transactional
    public @NonNull Market save(@NonNull Market market) {
        var entity = mapper.toEntity(market);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}

// Spring Data JPA Repository
interface MarketJpaRepository extends JpaRepository<MarketEntity, UUID> {
    
    Optional<MarketEntity> findBySlug(String slug);
    
    List<MarketEntity> findByStatus(String status);
    
    @Query("""
        SELECT m FROM MarketEntity m 
        WHERE m.status = :status 
        AND m.createdAt > :since
        ORDER BY m.createdAt DESC
        """)
    List<MarketEntity> findRecentByStatus(
        @Param("status") String status,
        @Param("since") Instant since
    );
}
```

---

## 2. 캐싱 패턴 (Decorator + Cache-Aside)

### 원본 TypeScript

```typescript
class CachedMarketRepository implements MarketRepository {
  constructor(
    private baseRepo: MarketRepository,
    private redis: RedisClient
  ) {}

  async findById(id: string): Promise<Market | null> {
    const cached = await this.redis.get(`market:${id}`)
    if (cached) {
      return JSON.parse(cached)
    }
    const market = await this.baseRepo.findById(id)
    if (market) {
      await this.redis.setex(`market:${id}`, 300, JSON.stringify(market))
    }
    return market
  }
}
```

### Java 버전

```java
package com.example.market.infrastructure.cache;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

/**
 * MarketRepository의 캐싱 데코레이터.
 * 
 * Cache-Aside 패턴:
 * 1. 캐시 확인 → 2. 캐시 미스 시 DB 조회 → 3. 결과 캐싱
 */
@Component
public class CachedMarketRepository implements MarketRepository {
    
    private static final String CACHE_NAME = "markets";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    private final MarketRepository delegate;
    private final RedisTemplate<String, Market> redisTemplate;
    
    public CachedMarketRepository(
        @Qualifier("jpaMarketRepository") MarketRepository delegate,
        RedisTemplate<String, Market> redisTemplate
    ) {
        this.delegate = delegate;
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    @Cacheable(value = CACHE_NAME, key = "#id.value()", unless = "#result == null")
    public @NonNull Optional<Market> findById(@NonNull MarketId id) {
        return delegate.findById(id);
    }
    
    @Override
    @CacheEvict(value = CACHE_NAME, key = "#market.id().value()")
    @Transactional
    public @NonNull Market save(@NonNull Market market) {
        return delegate.save(market);
    }
    
    @Override
    @CacheEvict(value = CACHE_NAME, key = "#id.value()")
    public void delete(@NonNull MarketId id) {
        delegate.delete(id);
    }
    
    /**
     * 특정 마켓의 캐시를 무효화합니다.
     */
    public void invalidateCache(@NonNull MarketId id) {
        var key = CACHE_NAME + "::" + id.value();
        redisTemplate.delete(key);
    }
    
    /**
     * 전체 마켓 캐시를 무효화합니다.
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void invalidateAllCache() {
        // Spring이 자동으로 처리
    }
}
```

### 캐시 설정

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            )
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("markets", 
                defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("prices", 
                defaultConfig.entryTtl(Duration.ofSeconds(30)))
            .build();
    }
}
```

---

## 3. API Response 패턴 (Result Type)

### 원본 TypeScript

```typescript
type ApiResponse<T> = 
  | { success: true; data: T }
  | { success: false; error: string }
```

### Java 버전 (sealed interface + record)

```java
package com.example.shared.kernel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.function.Function;

/**
 * 연산 결과를 나타내는 sealed 타입.
 * 예외 대신 명시적인 성공/실패를 표현합니다.
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {
    
    boolean isSuccess();
    boolean isFailure();
    
    @NonNull T getOrThrow();
    @Nullable T getOrNull();
    @NonNull T getOrElse(@NonNull T defaultValue);
    
    <U> @NonNull Result<U> map(@NonNull Function<T, U> mapper);
    <U> @NonNull Result<U> flatMap(@NonNull Function<T, Result<U>> mapper);
    
    // 성공 케이스
    record Success<T>(@NonNull T value) implements Result<T> {
        
        @Override public boolean isSuccess() { return true; }
        @Override public boolean isFailure() { return false; }
        
        @Override public @NonNull T getOrThrow() { return value; }
        @Override public @Nullable T getOrNull() { return value; }
        @Override public @NonNull T getOrElse(@NonNull T defaultValue) { return value; }
        
        @Override
        public <U> @NonNull Result<U> map(@NonNull Function<T, U> mapper) {
            return new Success<>(mapper.apply(value));
        }
        
        @Override
        public <U> @NonNull Result<U> flatMap(@NonNull Function<T, Result<U>> mapper) {
            return mapper.apply(value);
        }
    }
    
    // 실패 케이스
    record Failure<T>(@NonNull ErrorInfo error) implements Result<T> {
        
        @Override public boolean isSuccess() { return false; }
        @Override public boolean isFailure() { return true; }
        
        @Override public @NonNull T getOrThrow() { throw new ResultException(error); }
        @Override public @Nullable T getOrNull() { return null; }
        @Override public @NonNull T getOrElse(@NonNull T defaultValue) { return defaultValue; }
        
        @Override @SuppressWarnings("unchecked")
        public <U> @NonNull Result<U> map(@NonNull Function<T, U> mapper) {
            return (Result<U>) this;
        }
        
        @Override @SuppressWarnings("unchecked")
        public <U> @NonNull Result<U> flatMap(@NonNull Function<T, Result<U>> mapper) {
            return (Result<U>) this;
        }
    }
    
    // 팩토리 메서드
    static <T> @NonNull Result<T> success(@NonNull T value) {
        return new Success<>(value);
    }
    
    static <T> @NonNull Result<T> failure(@NonNull String message) {
        return new Failure<>(new ErrorInfo(message, null));
    }
    
    static <T> @NonNull Result<T> failure(@NonNull String message, @NonNull Throwable cause) {
        return new Failure<>(new ErrorInfo(message, cause));
    }
}

record ErrorInfo(@NonNull String message, @Nullable Throwable cause) {}
```

### 컨트롤러에서 사용

```java
@RestController
@RequestMapping("/api/v1/markets")
public class MarketController {
    
    private final MarketService marketService;
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MarketResponse>> getMarket(@PathVariable UUID id) {
        var result = marketService.findById(MarketId.of(id));
        
        return switch (result) {
            case Result.Success<Market> s -> ResponseEntity.ok(
                ApiResponse.success(MarketResponse.from(s.value()))
            );
            case Result.Failure<Market> f -> ResponseEntity.status(404).body(
                ApiResponse.error(f.error().message())
            );
        };
    }
}

// API Response DTO
public sealed interface ApiResponse<T> permits ApiResponse.SuccessResponse, ApiResponse.ErrorResponse {
    
    record SuccessResponse<T>(boolean success, @NonNull T data) implements ApiResponse<T> {
        public SuccessResponse { success = true; }
    }
    
    record ErrorResponse<T>(boolean success, @NonNull String error) implements ApiResponse<T> {
        public ErrorResponse { success = false; }
    }
    
    static <T> ApiResponse<T> success(T data) { return new SuccessResponse<>(true, data); }
    static <T> ApiResponse<T> error(String message) { return new ErrorResponse<>(false, message); }
}
```

---

## 4. HTTP Service Client (Spring Boot 4.0)

### 원본 TypeScript

```typescript
const response = await fetch('https://api.openai.com/v1/embeddings', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${apiKey}` },
  body: JSON.stringify({ input: text, model: 'text-embedding-3-small' })
})
```

### Java 버전 (선언적 HTTP 클라이언트)

```java
package com.example.market.infrastructure.external;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * OpenAI API 클라이언트.
 * Spring Boot 4.0의 선언적 HTTP 인터페이스를 사용합니다.
 */
@HttpExchange(url = "https://api.openai.com/v1")
public interface OpenAiClient {
    
    @PostExchange("/embeddings")
    EmbeddingResponse createEmbedding(@RequestBody EmbeddingRequest request);
    
    @PostExchange("/chat/completions")
    ChatCompletionResponse createChatCompletion(@RequestBody ChatCompletionRequest request);
}

record EmbeddingRequest(
    @NonNull String model,
    @NonNull String input
) {
    public static EmbeddingRequest of(String text) {
        return new EmbeddingRequest("text-embedding-3-small", text);
    }
}

record EmbeddingResponse(
    @NonNull List<EmbeddingData> data,
    @NonNull Usage usage
) {
    record EmbeddingData(int index, @NonNull List<Double> embedding) {}
    record Usage(int promptTokens, int totalTokens) {}
}
```

### 클라이언트 설정

```java
@Configuration
public class HttpClientConfiguration {
    
    @Bean
    public OpenAiClient openAiClient(
        @Value("${openai.api-key}") String apiKey,
        RestClient.Builder restClientBuilder
    ) {
        var restClient = restClientBuilder
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
        
        var factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();
        
        return factory.createClient(OpenAiClient.class);
    }
}
```

---

## 5. 트랜잭션 패턴 (Unit of Work)

```java
@Service
@Transactional
public class MarketApplicationService {
    
    private final MarketRepository marketRepository;
    private final EventPublisher eventPublisher;
    private final MarketDomainService domainService;
    
    /**
     * 마켓을 생성합니다.
     * 
     * 트랜잭션 내에서:
     * 1. 도메인 객체 생성
     * 2. 비즈니스 규칙 검증
     * 3. 저장
     * 4. 도메인 이벤트 발행
     */
    public MarketId createMarket(CreateMarketCommand command) {
        // 도메인 규칙 검증
        domainService.validateCreation(command);
        
        // 애그리거트 생성 (도메인 이벤트 기록)
        var market = Market.create(
            command.title(),
            command.description(),
            command.closeAt()
        );
        
        // 저장
        var saved = marketRepository.save(market);
        
        // 도메인 이벤트 발행 (트랜잭션 커밋 후)
        saved.domainEvents().forEach(eventPublisher::publish);
        saved.clearDomainEvents();
        
        return saved.id();
    }
}
```

---

**Remember: 패턴은 문제 해결을 위한 도구입니다. 패턴 자체가 목적이 되어서는 안 됩니다.**
