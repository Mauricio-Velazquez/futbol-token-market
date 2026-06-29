package com.futbol.tokenmarket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi applicationApi() {
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi monitoringApi() {
        return GroupedOpenApi.builder()
                .group("monitoring")
                .pathsToMatch("/actuator/**")
                .addOpenApiCustomizer(monitoringDescription())
                .build();
    }

    private OpenApiCustomizer monitoringDescription() {
        return openApi -> openApi.info(new Info()
                .title("Monitoring & Actuator")
                .version("1.0.0")
                .description("""
                        ## Endpoints de monitoreo y métricas

                        ---

                        ### `/actuator/health`
                        Estado general de la app. Incluye:
                        - **db** — conexión H2
                        - **diskSpace** — espacio libre
                        - **tokenMarket** — directorio de datos (`./data`)

                        ---

                        ### `/actuator/metrics/{nombre}`
                        Consulta individual de métricas. Soporta filtro por `?tag=clave:valor`.

                        #### Métricas custom del negocio
                        | Nombre | Tipo | Descripción |
                        |---|---|---|
                        | `market.players.total` | Gauge | Total de jugadores en el mercado |
                        | `market.token.price.avg` | Gauge | Precio promedio según estrategia activa |
                        | `market.token.price.max` | Gauge | Precio máximo del ranking actual |
                        | `market.token.price.min` | Gauge | Precio mínimo del ranking actual |
                        | `market.orders.processed` | Counter | Órdenes procesadas — filtrar con `?tag=type:BUY` o `?tag=type:SELL` |

                        #### Cache del ranking (Caffeine)
                        | Nombre | Tag útil |
                        |---|---|
                        | `cache.gets` | `?tag=name:ranking&tag=result:hit` / `result:miss` |
                        | `cache.puts` | `?tag=name:ranking` |
                        | `cache.evictions` | `?tag=name:ranking` |

                        #### HTTP requests
                        | Nombre | Ejemplo de tag |
                        |---|---|
                        | `http.server.requests` | `?tag=uri:/api/players/ranking&tag=method:GET` |

                        #### JVM
                        `jvm.memory.used`, `jvm.gc.pause`, `jvm.threads.live`, `jvm.classes.loaded`

                        ---

                        ### `/actuator/prometheus`
                        Dump completo en formato texto para scraping con Prometheus/Grafana.

                        ---

                        ### `/actuator/loggers/{nombre}`
                        Consulta o cambia el nivel de log en caliente vía `POST`.
                        Ejemplo: `POST /actuator/loggers/com.futbol.tokenmarket` con body `{"configuredLevel":"DEBUG"}`

                        ---

                        ### `/actuator/scheduledtasks`
                        Muestra los cron del scraper:
                        - `0 0 2 * * MON` — actualización semanal
                        - `0 0 3 * * MON,FRI` — recálculo de cotizaciones
                        """));
    }

    @Bean
    public OpenAPI openAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Futbol Token Market API")
                        .version("1.0.0")
                        .description("API para gestionar un mercado de tokens de jugadores de fútbol con autenticación JWT")
                        .contact(new Contact()
                                .name("Futbol Token Market")
                                .url("https://github.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en el login. Incluir como: Authorization: Bearer <token>")));
    }
}

