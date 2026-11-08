package com.javarush.jira.common.internal.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@OpenAPIDefinition(
        info = @Info(
                title = "REST API documentation",
                version = "1.0",
                description = """
                        <a href='http://localhost/'>JavaRush Jira application</a><br>
                        <p>Получите accessToken через POST /api/auth/login и вставьте в Authorize.</p>
                        <p><b>Учётные записи демонстрационных данных:</b><br>
                        - user@gmail.com / password<br>
                        - admin@gmail.com / admin<br>
                        - guest@gmail.com / guest</p>
                        """,
                contact = @Contact(url = "https://javarush.com/about/contacts", email = "support@javarush.com")
        ),
        servers = {
                @Server(url = "${app.host-url}")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
                .group("REST API")
                .pathsToMatch("/api/**")
                .build();
    }
}
