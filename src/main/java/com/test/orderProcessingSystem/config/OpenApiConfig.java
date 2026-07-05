package com.test.orderProcessingSystem.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI orderProcessingOpenAPI(
            @Value("${application.name}") String appName,
            @Value("${application.version}") String version) {

        return new OpenAPI()
                .info(new Info()
                        .title(appName)
                        .version(version)
                        .description("REST APIs for the Order Processing System"))
                // Registers a JWT Bearer scheme so Swagger UI shows the "Authorize" button and sends
                // the token as "Authorization: Bearer <token>" on every request.
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
