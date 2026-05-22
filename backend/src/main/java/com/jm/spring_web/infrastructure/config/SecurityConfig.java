package com.jm.spring_web.infrastructure.config;

import com.jm.spring_web.infrastructure.security.JwtAuthenticationFilter;
import com.jm.spring_web.infrastructure.security.JwtProperties;
import com.jm.spring_web.infrastructure.security.LoginLockoutProperties;
import com.jm.spring_web.infrastructure.security.MfaProperties;
import com.jm.spring_web.infrastructure.security.ratelimit.AuthRateLimitFilter;
import com.jm.spring_web.infrastructure.email.IamEmailProperties;
import com.jm.spring_web.infrastructure.security.ratelimit.AuthRateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({
    JwtProperties.class,
    LoginLockoutProperties.class,
    AuthRateLimitProperties.class,
    IamEmailProperties.class
})
public class SecurityConfig {
    @Value("${app.security.actuator-public:true}")
    private boolean actuatorPublic;

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthRateLimitFilter authRateLimitFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(problemAuthEntryPoint(objectMapper))
                        .accessDeniedHandler(problemAccessDeniedHandler(objectMapper)))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/logout",
                            "/api/v1/auth/mfa/verify",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/v3/api-docs.yaml",
                            "/webjars/**",
                            "/favicon.ico",
                            "/actuator/health",
                            "/actuator/info")
                            .permitAll();

                    if (actuatorPublic) {
                        auth.requestMatchers("/actuator/metrics/**", "/actuator/prometheus").permitAll();
                    }

                    auth.requestMatchers("/api/v1/admin/**").hasRole("SUPER_ADMIN");
                    auth.requestMatchers("/api/v1/branches/**").hasAnyRole("SUPER_ADMIN", "APP_ADMIN");
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authRateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationEntryPoint problemAuthEntryPoint(ObjectMapper objectMapper) {
        return (request, response, ex) -> writeProblemDetail(
                response,
                request,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Authentication is required to access this resource",
                objectMapper);
    }

    @Bean
    AccessDeniedHandler problemAccessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, ex) -> writeProblemDetail(
                response,
                request,
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "You do not have permission to access this resource",
                objectMapper);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    private void writeProblemDetail(
            HttpServletResponse response,
            HttpServletRequest request,
            int status,
            String title,
            String detail,
            ObjectMapper objectMapper) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", URI.create("https://api.spring-web/errors/" + status).toString());
        body.put("title", title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());
        body.put("timestamp", Instant.now());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
