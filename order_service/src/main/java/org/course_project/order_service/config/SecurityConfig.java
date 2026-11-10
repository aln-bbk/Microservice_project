package org.course_project.order_service.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public JwtDecoder jwtDecoder(
      RSAPublicKey jwtPublicKey,
      @Value("${app.jwt.issuer}") String issuer
  ) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtPublicKey).build();

    OAuth2TokenValidator<Jwt> issuerValidator =
        JwtValidators.createDefaultWithIssuer(issuer);

    OAuth2TokenValidator<Jwt> accessTokenValidator = jwt -> {
      String role = jwt.getClaimAsString("role");
      Object userId = jwt.getClaim("userId");
      String subject = jwt.getSubject();

      boolean validRole = "ADMIN".equals(role) || "USER".equals(role);

      boolean validUserId = false;

      if (userId instanceof Number number) {
        validUserId = number.longValue() > 0
            && subject != null
            && subject.equals(number.toString());
      }

      boolean validEmail =
          jwt.getClaimAsString("email") != null
              && !jwt.getClaimAsString("email").isBlank();

      boolean validToken =
          "access".equals(jwt.getClaimAsString("tokenType"))
              && validRole
              && validUserId
              && validEmail;

      if (validToken) {
        return OAuth2TokenValidatorResult.success();
      }

      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(
              "invalid_token",
              "Invalid access token claims",
              null
          )
      );
    };

    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            issuerValidator,
            accessTokenValidator
        )
    );

    return decoder;
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter =
        new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
      String role = jwt.getClaimAsString("role");

      if (!"ADMIN".equals(role) && !"USER".equals(role)) {
        return List.of();
      }

      return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    });

    return converter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationConverter converter,
      ObjectMapper objectMapper
  ) throws Exception {

    return http
        .csrf(csrf -> csrf.disable())

        .sessionManagement(session ->
            session.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
            )
        )

        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/actuator/health",
                "/actuator/health/**"
            ).permitAll()
            .anyRequest().authenticated()
        )

        .oauth2ResourceServer(oauth -> oauth
            .jwt(jwt ->
                jwt.jwtAuthenticationConverter(converter)
            )
        )

        .exceptionHandling(exceptions -> exceptions

            .authenticationEntryPoint((request, response, ex) -> {
              response.setStatus(HttpStatus.UNAUTHORIZED.value());
              response.setContentType(
                  MediaType.APPLICATION_JSON_VALUE
              );

              objectMapper.writeValue(
                  response.getOutputStream(),
                  Map.of(
                      "status", 401,
                      "error", "Unauthorized",
                      "message", "Authentication is required"
                  )
              );
            })

            .accessDeniedHandler((request, response, ex) -> {
              response.setStatus(HttpStatus.FORBIDDEN.value());
              response.setContentType(
                  MediaType.APPLICATION_JSON_VALUE
              );

              objectMapper.writeValue(
                  response.getOutputStream(),
                  Map.of(
                      "status", 403,
                      "error", "Forbidden",
                      "message", "Access denied"
                  )
              );
            })
        )

        .build();
  }
}
