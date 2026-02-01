# CLAUDE.md - 프로젝트 설정 예시

> 이 파일은 Claude Code가 프로젝트를 이해하는 데 사용됩니다.
> 프로젝트 루트에 이 파일을 배치하세요.

---

## 프로젝트 개요

**프로젝트명**: Market Platform
**설명**: 예측 마켓 플랫폼
**기술 스택**: Java 25, Spring Boot 4.0, PostgreSQL, Redis

---

## 기술 스택

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation                           │
│              Spring MVC, REST API, WebSocket                │
├─────────────────────────────────────────────────────────────┤
│                      Application                            │
│                   Application Services                      │
├─────────────────────────────────────────────────────────────┤
│                        Domain                               │
│       Entities, Value Objects, Domain Services              │
├─────────────────────────────────────────────────────────────┤
│                     Infrastructure                          │
│            JPA, Redis, External APIs                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 디렉토리 구조

```
src/main/java/com/example/market/
├── domain/                 # 도메인 계층
│   ├── model/              # 엔티티, 값 객체
│   ├── event/              # 도메인 이벤트
│   ├── service/            # 도메인 서비스
│   └── repository/         # 저장소 인터페이스
├── application/            # 애플리케이션 계층
│   ├── service/            # 애플리케이션 서비스
│   ├── command/            # 명령 객체
│   └── query/              # 쿼리 객체
├── infrastructure/         # 인프라스트럭처 계층
│   ├── persistence/        # JPA 구현
│   ├── cache/              # Redis 캐시
│   └── external/           # 외부 API 클라이언트
└── presentation/           # 프레젠테이션 계층
    ├── rest/               # REST 컨트롤러
    └── dto/                # DTO
```

---

## 코딩 규칙

### 필수 규칙

1. **TDD**: 모든 비즈니스 로직에 테스트 먼저 작성
2. **불변성**: DTO와 값 객체는 `record`로 정의
3. **Null 안전**: `Optional` + JSpecify 어노테이션 사용
4. **보안**: 시크릿 하드코딩 금지, SQL 인젝션 방지

### 코드 크기 제한

- 클래스: 400줄 이하
- 메서드: 50줄 이하
- 파라미터: 4개 이하
- 중첩 깊이: 3단계 이하

### 테스트

- 커버리지: 80% 이상
- 패턴: Given-When-Then
- 이름: 한글 `@DisplayName` 권장

---

## 에이전트 사용

### 코드 리뷰 요청 시

```
@code-reviewer 이 PR을 리뷰해주세요.
```

### 보안 검사 요청 시

```
@security-reviewer 이 코드의 보안 취약점을 검사해주세요.
```

### 아키텍처 결정 시

```
@architect 이 기능의 아키텍처를 설계해주세요.
```

---

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 커버리지 검증
./gradlew jacocoTestCoverageVerification

# 실행
./gradlew bootRun
```

---

## 환경 변수

```properties
# application.yml에서 사용
OPENAI_API_KEY=           # OpenAI API 키
DATABASE_URL=             # PostgreSQL URL
REDIS_URL=                # Redis URL
JWT_SECRET=               # JWT 서명 키 (256비트 이상)
```

---

## 자주 사용하는 명령

### TDD 워크플로우

```
/tdd MarketService.createMarket 메서드를 TDD로 구현해주세요.
```

### 리팩터링

```
/refactor-clean 이 클래스의 긴 메서드를 분리해주세요.
```

### 빌드 오류 해결

```
/build-fix 이 컴파일 오류를 해결해주세요.
```

---

## 참고 문서

- [skills/coding-standards.md](../skills/coding-standards.md) - 코딩 표준
- [skills/backend-patterns.md](../skills/backend-patterns.md) - 백엔드 패턴
- [rules/security.md](../rules/security.md) - 보안 규칙
- [rules/testing.md](../rules/testing.md) - 테스팅 규칙

---

## 체크리스트

PR 제출 전 확인:

- [ ] 테스트가 모두 통과하는가?
- [ ] 커버리지가 80% 이상인가?
- [ ] 보안 규칙을 준수하는가?
- [ ] 코드 스타일 규칙을 따르는가?
- [ ] 문서가 업데이트되었는가?
