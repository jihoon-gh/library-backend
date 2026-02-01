# Security Rules - Java 25 Edition

> 원본: rules/security.md
> 
> 프로젝트 전체에 적용되는 보안 규칙

---

## 규칙 1: 시크릿은 절대 하드코딩하지 않는다

### ❌ 절대 금지

```java
private static final String API_KEY = "sk-abc123";
private static final String DB_PASSWORD = "password123";
private static final String JWT_SECRET = "my-secret-key";
```

### ✅ 환경 변수 사용

```java
@Value("${api.key}")
private String apiKey;

@Value("${db.password}")
private String dbPassword;

@Value("${jwt.secret}")
private String jwtSecret;
```

### ✅ Spring Boot 4.0 ConfigurationProperties (권장)

```java
@ConfigurationProperties(prefix = "app.secrets")
public record SecretsConfig(
    String apiKey,
    String dbPassword,
    String jwtSecret
) {
    public SecretsConfig {
        Objects.requireNonNull(apiKey, "API key is required");
        Objects.requireNonNull(dbPassword, "DB password is required");
        Objects.requireNonNull(jwtSecret, "JWT secret is required");
    }
}
```

---

## 규칙 2: 모든 사용자 입력은 검증한다

### ❌ 검증 없음

```java
public User createUser(String email, String name) {
    return new User(email, name);  // 위험!
}
```

### ✅ Bean Validation 사용

```java
public record CreateUserRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    String email,
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 100, message = "이름은 2-100자 사이여야 합니다")
    String name,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 8자 이상, 문자/숫자/특수문자를 포함해야 합니다"
    )
    String password
) {}

@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    // 검증 통과 후 실행
    return ResponseEntity.ok(userService.create(request));
}
```

---

## 규칙 3: SQL 인젝션을 방지한다

### ❌ 문자열 연결

```java
String sql = "SELECT * FROM users WHERE name = '" + name + "'";
```

### ✅ 파라미터 바인딩

```java
// JdbcTemplate
String sql = "SELECT * FROM users WHERE name = ?";
jdbcTemplate.query(sql, userRowMapper, name);

// Named Parameter
String sql = "SELECT * FROM users WHERE name = :name";
namedParameterJdbcTemplate.query(sql, Map.of("name", name), userRowMapper);
```

### ✅ Spring Data JPA (권장)

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 메서드 이름 기반 쿼리 (자동으로 안전)
    List<User> findByName(String name);
    
    // JPQL (파라미터 바인딩)
    @Query("SELECT u FROM User u WHERE u.name = :name")
    List<User> findByNameQuery(@Param("name") String name);
    
    // Native Query (파라미터 바인딩)
    @Query(value = "SELECT * FROM users WHERE name = :name", nativeQuery = true)
    List<User> findByNameNative(@Param("name") String name);
}
```

### ✅ Criteria API (동적 쿼리)

```java
public List<User> findByDynamicCriteria(UserSearchCriteria criteria) {
    var cb = entityManager.getCriteriaBuilder();
    var query = cb.createQuery(User.class);
    var root = query.from(User.class);
    
    var predicates = new ArrayList<Predicate>();
    
    if (criteria.name() != null) {
        predicates.add(cb.equal(root.get("name"), criteria.name()));
    }
    if (criteria.email() != null) {
        predicates.add(cb.like(root.get("email"), "%" + criteria.email() + "%"));
    }
    
    query.where(predicates.toArray(new Predicate[0]));
    return entityManager.createQuery(query).getResultList();
}
```

---

## 규칙 4: 인증/인가를 항상 확인한다

### ❌ 인증 없이 접근 허용

```java
@GetMapping("/admin/users")
public List<User> getAllUsers() {
    return userService.findAll();  // 누구나 접근 가능!
}
```

### ✅ Spring Security 사용

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    // 관리자만 접근
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userService.findAll();
    }
    
    // 관리자 또는 본인만 접근
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    // 결과 기반 권한 검사
    @GetMapping("/documents/{id}")
    @PostAuthorize("returnObject.owner == authentication.principal.username")
    public Document getDocument(@PathVariable Long id) {
        return documentService.findById(id);
    }
}
```

---

## 규칙 5: 안전한 암호화를 사용한다

### 비밀번호 해싱

```java
// ❌ MD5 (안전하지 않음)
DigestUtils.md5Hex(password);

// ❌ SHA-256 단독 (salt 없음)
DigestUtils.sha256Hex(password);

// ✅ BCrypt (권장)
private final PasswordEncoder encoder = new BCryptPasswordEncoder(12);

public String hash(String password) {
    return encoder.encode(password);
}

public boolean verify(String raw, String encoded) {
    return encoder.matches(raw, encoded);
}
```

### 난수 생성

```java
// ❌ java.util.Random (예측 가능)
new Random().nextLong();

// ✅ SecureRandom
private static final SecureRandom SECURE_RANDOM = new SecureRandom();

public String generateToken() {
    var bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
}
```

---

## 규칙 6: CRITICAL 이슈는 즉시 수정한다

### 우선순위

1. **CRITICAL**: 즉시 수정 (배포 차단)
   - 하드코딩된 시크릿
   - SQL 인젝션
   - 인증 우회

2. **HIGH**: 24시간 내 수정
   - 취약한 암호화
   - 불충분한 입력 검증

3. **MEDIUM**: 1주일 내 수정
   - 보안 설정 미흡
   - 로깅 부족

4. **LOW**: 백로그에 추가
   - 개선 가능한 보안 설정

---

## 보안 체크리스트

코드 작성 시 확인하세요:

- [ ] 시크릿이 코드에 하드코딩되어 있지 않은가?
- [ ] 모든 사용자 입력이 검증되는가?
- [ ] SQL 쿼리가 파라미터 바인딩을 사용하는가?
- [ ] API 엔드포인트에 적절한 인증/인가가 있는가?
- [ ] 비밀번호가 BCrypt로 해싱되는가?
- [ ] 토큰 생성에 SecureRandom을 사용하는가?
- [ ] HTTPS가 강제되는가?
- [ ] CORS 설정이 적절한가?

---

**Remember: 보안은 기능이 아니라 요구사항입니다.**
