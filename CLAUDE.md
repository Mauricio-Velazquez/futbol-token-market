# futbol-token-market — guía para Claude

## Reglas de calidad de código (SonarCloud)

Este proyecto usa SonarCloud para análisis estático. Antes de tocar cualquier archivo Java, leer:

→ [docs/sonarcloud_decisions.md](docs/sonarcloud_decisions.md)

Contiene las 22 reglas aplicadas al proyecto con ejemplos de código correcto e incorrecto. Las más críticas:

- Usar SLF4J (`log.info/error`) — nunca `System.out` ni `System.err`
- `InterruptedException` siempre re-interrumpe el thread (`Thread.currentThread().interrupt()`)
- Constantes para strings repetidos (> 2 veces)
- CORS nunca con `origins = "*"` — configurar en `SecurityConfig`
- `Files.delete()` en lugar de `File.delete()`
- Switch siempre con `default`
- Tests: usar métodos específicos de AssertJ (`hasName`, `hasParent`, etc.)

## Stack

- Java 21 + Spring Boot
- Gradle
- Selenium + ChromeDriver (scraping de WhoScored)
- Jackson (JSON file-based persistence — sin base de datos)
- SLF4J + Logback (logging — sin Lombok)

## Estructura relevante

- `src/main/java/com/futbol/tokenmarket/service/WhoScoredScraperService.java` — scraper principal, archivo más complejo
- `src/main/java/com/futbol/tokenmarket/security/SecurityConfig.java` — CORS configurado acá, no en controllers
- `src/main/java/com/futbol/tokenmarket/exception/GlobalExceptionHandler.java` — maneja excepciones globalmente, los controllers pueden declarar `throws IOException`
