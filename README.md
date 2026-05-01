# futbol-token-market

## CI y SonarCloud

El proyecto incluye un workflow de GitHub Actions que ejecuta `./gradlew clean build` en cada push y pull request, y lanza SonarCloud en pushes o ejecuciones manuales.

Para habilitar SonarCloud en GitHub, configurar estas credenciales del repositorio:

- `SONAR_TOKEN` como secret.
- `SONAR_PROJECT_KEY` como repository variable.
- `SONAR_ORGANIZATION` como repository variable.

SonarCloud toma la cobertura desde `build/reports/jacoco/test/jacocoTestReport.xml`.