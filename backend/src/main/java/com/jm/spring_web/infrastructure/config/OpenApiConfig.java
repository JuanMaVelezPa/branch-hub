package com.jm.spring_web.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    /**
     * API document and JWT security scheme. Protected operations use
     * {@code @SecurityRequirement(name = "bearerAuth")} so public routes stay invokable without a token.
     */
    @Bean
    OpenAPI springWebOpenApi() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("spring-web API")
                        .version("v1")
                        .description("""
                                REST API for branches, IAM admin, and authentication.

                                **Auth:** `POST /api/v1/auth/login` returns either a **JWT access token** (and sets an HttpOnly refresh cookie) or a **short `mfaChallengeToken` JWT** when the user is a privileged administrator with TOTP enabled. Complete login with `POST /api/v1/auth/mfa/verify`.

                                **MFA enrollment:** `POST .../mfa/enroll/begin` and `.../confirm` require a bearer token (`SUPER_ADMIN` or `APP_ADMIN`).""")
                        .contact(new Contact().name("spring-web")))
                .components(new Components().addSecuritySchemes(
                        schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token from login or MFA verify")));
    }
}
