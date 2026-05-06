---
name: SonarCloud decisions for futbol-token-market
description: All SonarCloud rules applied to this project and how each was fixed, to avoid repeating issues in future sessions
type: feedback
originSessionId: 74bc774c-a863-488e-aa6f-4ad56804924a
---
# SonarCloud Decisions — futbol-token-market

All issues come from a SonarCloud analysis run on the project. Each section shows the rule, affected file(s), and the exact fix applied.

---

## 1. Replace System.out / System.err with a logger

**Rule:** `java:S106` — Standard outputs should not be used directly to log anything.

**Affected files:** `PlayerMatchStatsRepository`, `TeamRepository`, `PlayerService`, `ScraperSchedulerService`, `ScraperTriggerService`, `ScraperTriggerService`, `WhoScoredScraperService`

**Fix:** Add `private static final Logger log = LoggerFactory.getLogger(ClassName.class);` (SLF4J, no Lombok in this project) and replace every `System.out.println(...)` / `System.err.println(...)` with `log.info(...)` / `log.error(...)`.

```java
// WRONG
System.out.println("[PlayerService] " + league + ": done");
System.err.println("[WhoScored] Error: " + e.getMessage());

// CORRECT
private static final Logger log = LoggerFactory.getLogger(PlayerService.class);
log.info("[PlayerService] {}: done", league);
log.error("[WhoScored] Error: {}", e.getMessage(), e);
```

**Why:** SonarCloud treats stdout/stderr as a reliability and maintainability issue; SLF4J allows log-level control at runtime.

---

## 2. InterruptedException must re-interrupt the thread

**Rule:** `java:S2142` — "InterruptedException" should not be ignored.

**Affected file:** `WhoScoredScraperService` (every `Thread.sleep` call)

**Fix:** Never use `catch (InterruptedException ignored) {}`. Always call `Thread.currentThread().interrupt()` inside the catch. Best approach: extract all sleeps to a `sleepMs(long ms)` helper.

```java
// WRONG
try { Thread.sleep(500); } catch (InterruptedException ignored) {}

// CORRECT — extract to a helper
private void sleepMs(long ms) {
    try {
        Thread.sleep(ms);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

**Why:** Swallowing `InterruptedException` breaks thread cancellation. The rule fires both for the empty catch AND for not re-interrupting.

---

## 3. Empty catch blocks must have a comment or real code

**Rule:** `java:S108` — Nested blocks of code should not be left empty.

**Fix:** Either re-interrupt (see rule 2), add a log line, or add a comment explaining why the block is intentionally empty.

```java
// WRONG
} catch (Exception ignored) {}

// CORRECT
} catch (Exception ignored) {
    // fallback to hash — URL format not recognized
}
```

---

## 4. Define constants for repeated string literals

**Rule:** `java:S1192` — String literals should not be duplicated.

**Affected files:** `PlayerService` (prefix `"[PlayerService] "` × 5), `WhoScoredScraperService` (`"[WhoScored] "` × 8, `"\\|\\|\\|"` × 6, `"[WhoScored] Partido "` × 4)

**Fix:** Extract to `private static final String` constants.

```java
// WRONG — repeated inline
System.out.println("[PlayerService] " + league + ": ...");

// CORRECT
private static final String LOG_PREFIX = "[PlayerService] ";
log.info(LOG_PREFIX + "{}: ...", league);

// For WhoScoredScraperService
private static final String WS_PREFIX  = "[WhoScored] ";
private static final String SEPARATOR  = "\\|\\|\\|";
private static final String MATCH_PREFIX = "[WhoScored] Partido ";
```

---

## 5. Use Files.delete() instead of File.delete()

**Rule:** `java:S4042` — "java.nio.file.Files#delete" should be preferred over "File#delete".

**Affected files:** `PlayerMatchStatsRepository`, `PlayerService`

**Fix:** Replace `file.delete()` with `Files.delete(file.toPath())` (import `java.nio.file.Files`). This throws `IOException` on failure instead of silently returning false.

```java
// WRONG
tempFile.delete();

// CORRECT
Files.delete(tempFile.toPath());
```

**Why:** `File.delete()` returns a boolean that is easily ignored; `Files.delete()` throws on failure, making errors visible.

---

## 6. Do something with the boolean returned by delete()

**Rule:** `java:S4165` — The return value of "delete" should not be ignored.

**Fix:** Covered by using `Files.delete()` (see rule 5). If for some reason `File.delete()` must be kept, check the return value explicitly.

---

## 7. Replace generic Exception with specific exceptions (controllers)

**Rule:** `java:S112` — Generic exceptions should never be thrown.

**Affected file:** `AdminController` (was catching `Exception` and rethrowing as `RuntimeException`)

**Fix:** Catch the actual checked exception that the service declares.

```java
// WRONG
} catch (Exception e) {
    e.printStackTrace();
    throw new RuntimeException(e.getMessage(), e);
}

// CORRECT
} catch (IOException e) {
    log.error("Error scrapeando equipos para liga {}: {}", league, e.getMessage(), e);
    return ResponseEntity.internalServerError().build();
}
```

---

## 8. Remove e.printStackTrace()

**Rule:** `java:S1148` — "printStackTrace" should not be called.

**Fix:** Replace with `log.error("...", e)` — pass the exception as the last argument so SLF4J captures the full stack trace.

---

## 9. CORS: don't use origins = "*"

**Rule:** `java:S5122` — Enabling CORS is safe only with restricted origins.

**Affected file:** `AuthController` had `@CrossOrigin(origins = "*", maxAge = 3600)`

**Fix:**
- Remove `@CrossOrigin` from controllers entirely.
- Configure CORS centrally in `SecurityConfig` with a `CorsConfigurationSource` bean using explicit origins from a configurable property.

```java
// In SecurityConfig
@Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
private List<String> allowedOrigins;

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}

// Wire into filter chain
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

---

## 10. Set an HttpStatus code reflective of the operation

**Rule:** `java:S6242` — Controller methods should return a status code that matches the operation.

**Affected file:** `PlayerController` — was catching `IOException` and returning 500 explicitly.

**Fix:** Since `GlobalExceptionHandler` already handles `Exception` → 500, just propagate the exception. Declare `throws IOException` on the method and remove the try-catch.

```java
// WRONG
public ResponseEntity<List<Player>> getAllPlayers(...) {
    try {
        return ResponseEntity.ok(service.getFilteredPlayers(...));
    } catch (IOException e) {
        return ResponseEntity.internalServerError().build();
    }
}

// CORRECT — GlobalExceptionHandler covers it
public ResponseEntity<List<Player>> getAllPlayers(...) throws IOException {
    return ResponseEntity.ok(service.getFilteredPlayers(...));
}
```

---

## 11. Empty constructors in model classes need a comment

**Rule:** `java:S1186` — Methods should not be empty.

**Affected files:** `Player.java`, `PlayerMatchStats.java`

**Fix:** Add a comment explaining why the constructor is empty (Jackson requires it for deserialization).

```java
// WRONG
public Player() {}

// CORRECT
// required by Jackson for deserialization
public Player() {}
```

---

## 12. Use IntConsumer / DoubleConsumer instead of Consumer<Integer> / Consumer<Double>

**Rule:** `java:S4276` — Functional interfaces should be as specialised as possible.

**Affected file:** `WhoScoredScraperService` — `trySetIntStat` and `trySetDoubleStat`

**Fix:**

```java
// WRONG
private void trySetIntStat(java.util.function.Consumer<Integer> setter, String val) { ... }
private void trySetDoubleStat(java.util.function.Consumer<Double> setter, String val) { ... }

// CORRECT
import java.util.function.IntConsumer;
import java.util.function.DoubleConsumer;

private void trySetIntStat(IntConsumer setter, String val) { ... }
private void trySetDoubleStat(DoubleConsumer setter, String val) { ... }
```

---

## 13. Use concise character class syntax

**Rule:** `java:S5361` — Use `\D` instead of `[^0-9]`.

**Fix:**

```java
// WRONG
val.replaceAll("[^0-9]", "");

// CORRECT
val.replaceAll("\\D", "");
```

---

## 14. Math.abs() on hashCode can still be negative

**Rule:** `java:S2676` — `Math.abs` should not be used on values that may be `Integer.MIN_VALUE`.

**Affected file:** `WhoScoredScraperService` — `extractPlayerIdFromUrl`, `extractMatchIdFromUrl`

**Fix:** Use `& Integer.MAX_VALUE` to strip the sign bit reliably.

```java
// WRONG
return String.valueOf(Math.abs(href.hashCode()));

// CORRECT
return String.valueOf(href.hashCode() & Integer.MAX_VALUE);
```

---

## 15. Switch statements must have a default case

**Rule:** `java:S131` — "switch" statements should have a "default" clause.

**Affected file:** `WhoScoredScraperService` — `mapStat()` switch

**Fix:**

```java
switch (key.toLowerCase()) {
    case "position" -> s.setPosition(val);
    // ... all cases ...
    default -> { /* unknown stat key, intentionally ignored */ }
}
```

---

## 16. Reduce cognitive complexity (extract methods)

**Rule:** `java:S3776` — Cognitive Complexity should not exceed 15.

**Affected file:** `WhoScoredScraperService` — three methods exceeded the limit (19, 18, 30).

**Fix:** Extract nested logic into private helper methods. Key extractions done:
- `parseTeamEntries(captured, teamUrls)` — out of `scrapeTeamUrls`
- `parsePlayers(captured, team, seenPlayerIds, players)` — out of `scrapePlayersFromTeam`
- `buildStatsMap(summaryRows, existingMatchIds, player)` — out of `scrapePlayerMatchStats`
- `enrichStatsWithTabs(...)` + `processTab(...)` — out of `scrapePlayerMatchStats`
- `processExtraction(...)` — out of `scrapeMatchPlayerStats`
- `pollItems(js, script, maxAttempts, label)` — unified polling helper (replaces inline for-loops)
- `sleepMs(ms)` — unified sleep helper (fixes InterruptedException everywhere)

**How to apply:** When a method has deeply nested loops + try-catch + conditionals, extract each logical "phase" into a private method. Aim for methods that fit on one screen.

---

## 17. Extract nested try blocks into separate methods

**Rule:** `java:S1141` — Nested try statements should not be used.

**Fix:** Same as rule 16 — `processTab()`, `processExtraction()`, and `sleepMs()` resolve all nested try issues.

---

## 18. At most one break/continue per loop

**Rule:** `java:S135` — Loops should not contain more than a single "break" or "continue" statement.

**Fix:** Consolidate into a single conditional branch.

```java
// WRONG — two continues
for (String entry : captured) {
    if (parts.length < 3 || parts[1].trim().isEmpty()) continue;
    if (seenPlayerIds.contains(playerId)) continue;
    // ...
}

// CORRECT — one continue, second converted to if-block
for (String entry : captured) {
    if (parts.length < 3 || parts[1].trim().isEmpty()) continue;
    if (!seenPlayerIds.contains(playerId)) {
        // ...
    }
}
```

---

## 19. Remove unused imports

**Rule:** `java:S1128` — Unnecessary imports should be removed.

**Affected file:** `WhoScoredScraperService` had `import io.github.bonigarcia.wdm.WebDriverManager;` unused.

**Fix:** Delete the import line.

---

## 20. Remove commented-out code

**Rule:** `java:S125` — Sections of code should not be "commented out".

**Fix:** Delete the commented block entirely. If needed for history, it's in git.

---

## 21. Test assertions: use specific AbstractFileAssert methods

**Rule:** `java:S5785` — Use AssertJ's type-specific assertions instead of generic `.isEqualTo()` on extracted properties.

**Affected file:** `PlayerMatchStatsRepositoryTest`

**Fix:**

```java
// WRONG
assertThat(temp.getName()).isEqualTo("match_stats_Premier_League.tmp.json");
assertThat(temp.getParentFile()).isEqualTo(dataFile.getParentFile());

// CORRECT
assertThat(temp).hasName("match_stats_Premier_League.tmp.json");
assertThat(temp).hasParent(dataFile.getParentFile());
```

---

## 22. Brain Method detection

**Rule:** `java:S138` / Brain Method — A method is too long, complex, deeply nested, and uses too many variables.

**Fix:** Same as rule 16. SonarCloud's thresholds: LOC ≤ 64, Complexity ≤ 14, Nesting ≤ 2, Variables ≤ 6. Extract phases into private helpers.

---

## General pattern — how to approach SonarCloud fixes

1. **Logging:** Always use SLF4J (no Lombok in this project). One `log` field per class.
2. **Exceptions:** Never catch and ignore `InterruptedException` without re-interrupting. Prefer specific exceptions over `Exception`.
3. **Constants:** Any string literal used > 2 times → extract to `private static final String`.
4. **Complexity:** When a method has nested loops + try-catch → extract each logical block to a private method.
5. **Tests:** Use AssertJ's domain-specific assertions (`hasName`, `hasParent`, `hasSize`, etc.) instead of extracting a property and calling `isEqualTo`.
6. **CORS:** Never `@CrossOrigin(origins = "*")` in controllers. Configure centrally in `SecurityConfig` with a `@Value`-driven list.
