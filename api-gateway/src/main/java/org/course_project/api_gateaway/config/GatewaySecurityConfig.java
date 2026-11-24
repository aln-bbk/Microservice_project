package org.course_project.api_gateaway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class GatewaySecurityConfig {

  @Bean
  RSAPublicKey gatewayPublicKey(
      @Value("${app.jwt.public-key-location}") Resource keyResource) throws Exception {
    try (InputStream input = keyResource.getInputStream()) {
      return RsaKeyConverters.x509().convert(input);
    }
  }

  @Bean
  ReactiveJwtDecoder jwtDecoder(RSAPublicKey gatewayPublicKey,
                                @Value("${app.jwt.issuer}") String issuer) {
    NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
        .withPublicKey(gatewayPublicKey)
        .build();

    OAuth2TokenValidator<Jwt> issuerAndTimeValidator =
        JwtValidators.createDefaultWithIssuer(issuer);
    OAuth2TokenValidator<Jwt> accessTokenValidator = jwt ->
        "access".equals(jwt.getClaimAsString("tokenType"))
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "Access token required", null));

    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        issuerAndTimeValidator, accessTokenValidator));
    return decoder;
  }

  @Bean
  SecurityWebFilterChain gatewaySecurity(ServerHttpSecurity http) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(HttpMethod.POST,
                "/api/auth/login",
                "/api/users/register"
            ).permitAll()
            .anyExchange().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }
}
