# Hooks System - Java 25 Edition

> 원본: hooks/hooks.json
> 
> 도구 이벤트에 반응하는 훅 시스템 (Java 25 구현 가이드)

---

## 훅이란?

훅(Hook)은 Claude Code가 특정 도구를 사용할 때 자동으로 실행되는 검사입니다.
코드 품질과 보안을 자동으로 강화합니다.

---

## 원본 TypeScript/Shell 훅

```json
{
  "matcher": "tool == \"Write\" && tool_input.file_path matches \"\\\\.(md|txt)$\"",
  "hooks": [{
    "type": "command",
    "command": "echo '[Hook] Checking documentation...'"
  }]
}
```

---

## Java 25 구현

### 훅 트리거 타입

```java
public enum HookTrigger {
    PRE_TOOL_USE,    // 도구 사용 전
    POST_TOOL_USE,   // 도구 사용 후
    SESSION_START,   // 세션 시작
    SESSION_END      // 세션 종료
}
```

### 훅 인터페이스

```java
public sealed interface Hook permits 
    PreToolUseHook, 
    PostToolUseHook, 
    SessionHook {
    
    String id();
    String description();
    HookTrigger trigger();
    boolean matches(HookContext context);
    HookResult execute(HookContext context);
}

// 컨텍스트
public record HookContext(
    String toolName,
    Map<String, Object> toolInput,
    Map<String, Object> sessionData
) {
    public Optional<String> filePath() {
        return Optional.ofNullable(toolInput.get("file_path"))
            .map(Object::toString);
    }
    
    public Optional<String> content() {
        return Optional.ofNullable(toolInput.get("content"))
            .map(Object::toString);
    }
}

// 결과
public sealed interface HookResult permits 
    HookResult.Continue, 
    HookResult.Block, 
    HookResult.Warn {
    
    record Continue(String message) implements HookResult {}
    record Block(String reason) implements HookResult {}
    record Warn(String warning) implements HookResult {}
}
```

---

## 훅 구현 예시

### 1. 불필요한 문서 파일 생성 차단

```java
/**
 * 불필요한 문서 파일 생성을 차단합니다.
 * 
 * README.md, CLAUDE.md 등 허용된 파일만 생성 가능합니다.
 */
public record BlockUnnecessaryDocsHook() implements PreToolUseHook {
    
    private static final Pattern DOC_PATTERN = Pattern.compile("\\.(md|txt)$");
    private static final Pattern ALLOWED_PATTERN = Pattern.compile(
        "(README|CLAUDE|AGENTS|CONTRIBUTING|CHANGELOG)\\.md$"
    );
    
    @Override
    public String id() { return "block-unnecessary-docs"; }
    
    @Override
    public String description() { return "불필요한 문서 파일 생성 차단"; }
    
    @Override
    public HookTrigger trigger() { return HookTrigger.PRE_TOOL_USE; }
    
    @Override
    public boolean matches(HookContext context) {
        if (!"Write".equals(context.toolName())) {
            return false;
        }
        return context.filePath()
            .filter(path -> DOC_PATTERN.matcher(path).find())
            .filter(path -> !ALLOWED_PATTERN.matcher(path).find())
            .isPresent();
    }
    
    @Override
    public HookResult execute(HookContext context) {
        var path = context.filePath().orElse("unknown");
        return new HookResult.Block(
            "[Hook] BLOCKED: 불필요한 문서 파일 생성\n" +
            "[Hook] File: " + path + "\n" +
            "[Hook] README.md를 대신 사용하세요"
        );
    }
}
```

### 2. System.out 사용 경고

```java
/**
 * Java 코드에서 System.out.println 사용 시 경고합니다.
 */
public record WarnSystemOutHook() implements PostToolUseHook {
    
    private static final Pattern JAVA_PATTERN = Pattern.compile("\\.java$");
    private static final Pattern SYSOUT_PATTERN = Pattern.compile(
        "System\\.(out|err)\\.(print|println)"
    );
    
    @Override
    public String id() { return "warn-system-out"; }
    
    @Override
    public String description() { return "System.out 사용 경고"; }
    
    @Override
    public HookTrigger trigger() { return HookTrigger.POST_TOOL_USE; }
    
    @Override
    public boolean matches(HookContext context) {
        if (!"Edit".equals(context.toolName()) && !"Write".equals(context.toolName())) {
            return false;
        }
        return context.filePath()
            .filter(path -> JAVA_PATTERN.matcher(path).find())
            .isPresent();
    }
    
    @Override
    public HookResult execute(HookContext context) {
        var content = context.content().orElse("");
        
        if (SYSOUT_PATTERN.matcher(content).find()) {
            return new HookResult.Warn(
                "[Hook] System.out 사용이 감지되었습니다.\n" +
                "[Hook] 프로덕션 코드에서는 Logger를 사용하세요:\n" +
                "  private static final Logger log = LoggerFactory.getLogger(MyClass.class);\n" +
                "  log.info(\"message\");"
            );
        }
        
        return new HookResult.Continue("OK");
    }
}
```

### 3. 보안 검사 훅

```java
/**
 * 하드코딩된 시크릿을 탐지합니다.
 */
public record SecurityCheckHook() implements PreToolUseHook {
    
    private static final List<Pattern> SECRET_PATTERNS = List.of(
        Pattern.compile("(?i)(api[_-]?key|apikey)\\s*=\\s*[\"'][^\"']{20,}[\"']"),
        Pattern.compile("AKIA[0-9A-Z]{16}"),
        Pattern.compile("(?i)(password|passwd|pwd)\\s*=\\s*[\"'][^\"']+[\"']"),
        Pattern.compile("-----BEGIN (RSA |EC )?PRIVATE KEY-----")
    );
    
    @Override
    public String id() { return "security-check"; }
    
    @Override
    public String description() { return "하드코딩된 시크릿 탐지"; }
    
    @Override
    public HookTrigger trigger() { return HookTrigger.PRE_TOOL_USE; }
    
    @Override
    public boolean matches(HookContext context) {
        return "Write".equals(context.toolName()) || "Edit".equals(context.toolName());
    }
    
    @Override
    public HookResult execute(HookContext context) {
        var content = context.content().orElse("");
        
        for (var pattern : SECRET_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return new HookResult.Block(
                    "[Hook] BLOCKED: 하드코딩된 시크릿이 감지되었습니다.\n" +
                    "[Hook] 환경 변수 또는 시크릿 매니저를 사용하세요.\n" +
                    "[Hook] 예: @Value(\"${api.key}\") String apiKey;"
                );
            }
        }
        
        return new HookResult.Continue("Security check passed");
    }
}
```

---

## 훅 프로세서 (Java 25 Structured Concurrency)

```java
@Service
public class HookProcessor {
    
    private final List<Hook> hooks;
    
    public HookProcessor(List<Hook> hooks) {
        this.hooks = List.copyOf(hooks);
    }
    
    /**
     * 훅을 병렬로 실행합니다.
     */
    public List<HookResult> process(HookContext context) {
        var matchingHooks = hooks.stream()
            .filter(hook -> hook.matches(context))
            .toList();
        
        if (matchingHooks.isEmpty()) {
            return List.of();
        }
        
        // Java 25 Structured Concurrency
        try (var scope = StructuredTaskScope.open()) {
            var subtasks = matchingHooks.stream()
                .map(hook -> scope.fork(() -> hook.execute(context)))
                .toList();
            
            scope.join();
            
            return subtasks.stream()
                .map(StructuredTaskScope.Subtask::get)
                .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of(new HookResult.Warn("Hook execution interrupted"));
        }
    }
    
    /**
     * Block 결과가 있으면 true 반환.
     */
    public boolean hasBlockingResult(List<HookResult> results) {
        return results.stream()
            .anyMatch(r -> r instanceof HookResult.Block);
    }
    
    /**
     * 모든 경고 메시지를 수집.
     */
    public List<String> collectWarnings(List<HookResult> results) {
        return results.stream()
            .filter(r -> r instanceof HookResult.Warn)
            .map(r -> ((HookResult.Warn) r).warning())
            .toList();
    }
}
```

---

## 훅 설정

```java
@Configuration
public class HookConfiguration {
    
    @Bean
    public List<Hook> hooks() {
        return List.of(
            new BlockUnnecessaryDocsHook(),
            new WarnSystemOutHook(),
            new SecurityCheckHook(),
            new TestCoverageHook()
        );
    }
    
    @Bean
    public HookProcessor hookProcessor(List<Hook> hooks) {
        return new HookProcessor(hooks);
    }
}
```

---

## 사용 예시

```java
@Service
public class ToolExecutionService {
    
    private final HookProcessor hookProcessor;
    
    public ToolResult executeTool(String toolName, Map<String, Object> input) {
        var context = new HookContext(toolName, input, Map.of());
        
        // Pre-hook 실행
        var preResults = hookProcessor.process(context);
        
        if (hookProcessor.hasBlockingResult(preResults)) {
            var blockResult = preResults.stream()
                .filter(r -> r instanceof HookResult.Block)
                .findFirst()
                .orElseThrow();
            return ToolResult.blocked(((HookResult.Block) blockResult).reason());
        }
        
        // 도구 실행
        var toolResult = executeToolInternal(toolName, input);
        
        // Post-hook 실행
        var postResults = hookProcessor.process(context);
        var warnings = hookProcessor.collectWarnings(postResults);
        
        if (!warnings.isEmpty()) {
            return toolResult.withWarnings(warnings);
        }
        
        return toolResult;
    }
}
```

---

## 훅 목록

| 훅 ID | 트리거 | 설명 |
|-------|--------|------|
| `block-unnecessary-docs` | PRE | 불필요한 문서 파일 차단 |
| `warn-system-out` | POST | System.out 사용 경고 |
| `security-check` | PRE | 하드코딩된 시크릿 차단 |
| `test-coverage` | POST | 테스트 누락 경고 |
| `import-check` | POST | 불필요한 import 경고 |

---

**Remember: 훅은 품질 게이트입니다. 문제를 조기에 발견하세요.**
