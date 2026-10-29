package org.course_project.authenticationService.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {

  @Bean
  public RSAPublicKey rsaPublicKey(
      @Value("${app.jwt.public-key-location}") String location
  ) throws IOException {
    try (InputStream input = Files.newInputStream(Path.of(location))) {
      return RsaKeyConverters.x509().convert(input);
    }
  }

  @Bean
  public RSAPrivateKey rsaPrivateKey(
      @Value("${app.jwt.private-key-location}") String location
  ) throws IOException {
    try (InputStream input = Files.newInputStream(Path.of(location))) {
      return RsaKeyConverters.pkcs8().convert(input);
    }
  }

  @Bean
  public JwtEncoder jwtEncoder(
      RSAPublicKey publicKey,
      RSAPrivateKey privateKey
  ) {
    RSAKey rsaKey = new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID("authentication-service-key")
        .build();

    ImmutableJWKSet<SecurityContext> jwkSource =
        new ImmutableJWKSet<>(new JWKSet(rsaKey));

    return new NimbusJwtEncoder(jwkSource);
  }

  /*
   * Декодер для JwtService:
   * проверяет подпись и стандартные ограничения JWT.
   * JwtService дополнительно проверяет issuer и tokenType.
   */
  @Bean("jwtTokenDecoder")
  public JwtDecoder jwtTokenDecoder(RSAPublicKey publicKey) {
    return NimbusJwtDecoder.withPublicKey(publicKey).build();
  }

  /*
   * Декодер для Spring Security:
   * разрешает только access-токены с корректным issuer.
   */
  @Bean("jwtDecoder")
  @Primary
  public JwtDecoder jwtDecoder(
      RSAPublicKey publicKey,
      @Value("${app.jwt.issuer}") String issuer
  ) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withPublicKey(publicKey).build();

    OAuth2TokenValidator<Jwt> issuerValidator =
        JwtValidators.createDefaultWithIssuer(issuer);

    OAuth2TokenValidator<Jwt> accessTypeValidator = jwt -> {
      if ("access".equals(jwt.getClaimAsString("tokenType"))) {
        return OAuth2TokenValidatorResult.success();
      }

      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error(
              "invalid_token",
              "Access token required",
              null
          )
      );
    };

    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            issuerValidator,
            accessTypeValidator
        )
    );

    return decoder;
  }
}