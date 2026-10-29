package org.course_project.authenticationService.config;

import org.course_project.authenticationService.exception.ApiError;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http
  ) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/validate",
                "/internal/credentials",
                "/actuator/health"
            ).permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, exception) -> {
              response.setStatus(HttpStatus.UNAUTHORIZED.value());
              response.setContentType("application/json");
              response.setCharacterEncoding("UTF-8");

              ApiError body = new ApiError(
                  Instant.now(),
                  HttpStatus.UNAUTHORIZED.value(),
                  "Unauthorized",
                  "Authentication is required or the token is invalid",
                  Map.of()
              );

              new ObjectMapper().writeValue(response.getOutputStream(), body);
            })
            .accessDeniedHandler((request, response, exception) -> {
              response.setStatus(HttpStatus.FORBIDDEN.value());
              response.setContentType("application/json");
              response.setCharacterEncoding("UTF-8");

              ApiError body = new ApiError(
                  Instant.now(),
                  HttpStatus.FORBIDDEN.value(),
                  "Forbidden",
                  "You do not have permission to perform this action",
                  Map.of()
              );

              new ObjectMapper().writeValue(response.getOutputStream(), body);
            }))
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults()))
        .build();
  }
}