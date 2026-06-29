package com.futbol.tokenmarket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
                .build();
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

