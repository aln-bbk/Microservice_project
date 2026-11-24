package org.course_project.api_gateaway;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.jwt.issuer=https://auth.example.com",
        "app.jwt.public-key-location=classpath:keys/public.pem",
        "AUTH_SERVICE_URL=http://127.0.0.1:1",
        "USER_SERVICE_URL=http://127.0.0.1:1",
        "ORDER_SERVICE_URL=http://127.0.0.1:1"
    }
)
@Import(GatewaySecurityIntegrationTest.JwtTestConfiguration.class)
class GatewaySecurityIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private JwtEncoder jwtEncoder;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  /*
   * PUBLIC ENDPOINTS
   */

  @Test
  void loginIsPublicAndRequestReachesDownstream() throws Exception {
    HttpResponse<String> response = post("/api/auth/login", null);

    // Login must not be rejected by Gateway security.
    // Downstream is intentionally unavailable, so we expect 5xx.
    assertDownstreamFailure(response);
  }

  @Test
  void registrationIsPublicAndRequestReachesDownstream() throws Exception {
    HttpResponse<String> response = post("/api/users/register", null);

    // Registration must not be rejected by Gateway security.
    assertDownstreamFailure(response);
  }

  /*
   * PUBLIC PATHS MUST NOT BE PUBLIC FOR OTHER HTTP METHODS
   */

  @Test
  void getLoginWithoutTokenIsUnauthorized() throws Exception {
    HttpResponse<String> response = get("/api/auth/login", null);

    assertEquals(401, response.statusCode());
  }

  @Test
  void getRegistrationWithoutTokenIsUnauthorized() throws Exception {
    HttpResponse<String> response = get("/api/users/register", null);

    assertEquals(401, response.statusCode());
  }

  /*
   * PROTECTED ENDPOINTS
   */

  @Test
  void protectedEndpointRejectsMissingToken() throws Exception {
    HttpResponse<String> response = get("/api/orders", null);

    assertEquals(401, response.statusCode());
  }

  @Test
  void actuatorEndpointAlsoRequiresToken() throws Exception {
    HttpResponse<String> response = get("/actuator/health", null);

    assertEquals(401, response.statusCode());
  }

  @Test
  void protectedEndpointRejectsMalformedToken() throws Exception {
    HttpResponse<String> response = get(
        "/api/orders",
        "this-is-not-a-jwt"
    );

    assertEquals(401, response.statusCode());
  }

  @Test
  void protectedEndpointRejectsExpiredToken() throws Exception {
    String expiredToken = token(
        "access",
        Instant.now().minusSeconds(600),
        Instant.now().minusSeconds(60),
        "https://auth.example.com"
    );

    HttpResponse<String> response = get("/api/orders", expiredToken);

    assertEquals(401, response.statusCode());
  }

  @Test
  void protectedEndpointRejectsTokenFromWrongIssuer() throws Exception {
    String wrongIssuerToken = token(
        "access",
        Instant.now().minusSeconds(5),
        Instant.now().plusSeconds(300),
        "https://untrusted.example.com"
    );

    HttpResponse<String> response = get(
        "/api/orders",
        wrongIssuerToken
    );

    assertEquals(401, response.statusCode());
  }

  @Test
  void protectedEndpointRejectsRefreshToken() throws Exception {
    String refreshToken = token(
        "refresh",
        Instant.now().minusSeconds(5),
        Instant.now().plusSeconds(300),
        "https://auth.example.com"
    );

    HttpResponse<String> response = get(
        "/api/orders",
        refreshToken
    );

    assertEquals(401, response.statusCode());
  }

  @Test
  void protectedEndpointRejectsTamperedSignature() throws Exception {
    String validToken = token(
        "access",
        Instant.now().minusSeconds(5),
        Instant.now().plusSeconds(300),
        "https://auth.example.com"
    );

    String tamperedToken = tamperSignature(validToken);

    HttpResponse<String> response = get(
        "/api/orders",
        tamperedToken
    );

    assertEquals(401, response.statusCode());
  }

  @Test
  void protectedEndpointAcceptsValidAccessToken() throws Exception {
    String accessToken = token(
        "access",
        Instant.now().minusSeconds(5),
        Instant.now().plusSeconds(300),
        "https://auth.example.com"
    );

    HttpResponse<String> response = get(
        "/api/orders",
        accessToken
    );

    // JWT validation should pass.
    // Downstream service is unavailable, so the Gateway should return 5xx.
    assertDownstreamFailure(response);
  }

  /*
   * HELPERS
   */

  private void assertDownstreamFailure(HttpResponse<String> response) {
    assertTrue(
        response.statusCode() >= 500 && response.statusCode() < 600,
        "Expected downstream 5xx error, but got HTTP "
            + response.statusCode()
            + ". Response body: "
            + response.body()
    );
  }

  private String tamperSignature(String token) {
    String[] parts = token.split("\\.");

    if (parts.length != 3) {
      throw new IllegalArgumentException("Token is not a JWT");
    }

    char replacement = parts[2].charAt(0) == 'A' ? 'B' : 'A';
    parts[2] = replacement + parts[2].substring(1);

    return String.join(".", parts);
  }

  private String token(
      String tokenType,
      Instant issuedAt,
      Instant expiresAt,
      String issuer
  ) {
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .subject("123")
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .claim("userId", 123L)
        .claim("role", "USER")
        .claim("tokenType", tokenType)
        .build();

    JwsHeader header = JwsHeader
        .with(SignatureAlgorithm.RS256)
        .build();

    return jwtEncoder.encode(
        JwtEncoderParameters.from(header, claims)
    ).getTokenValue();
  }

  private HttpResponse<String> get(
      String path,
      String bearerToken
  ) throws Exception {

    HttpRequest.Builder request = HttpRequest
        .newBuilder(URI.create(url(path)))
        .GET();

    if (bearerToken != null) {
      request.header("Authorization", "Bearer " + bearerToken);
    }

    return httpClient.send(
        request.build(),
        HttpResponse.BodyHandlers.ofString()
    );
  }

  private HttpResponse<String> post(
      String path,
      String bearerToken
  ) throws Exception {

    HttpRequest.Builder request = HttpRequest
        .newBuilder(URI.create(url(path)))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{}"));

    if (bearerToken != null) {
      request.header("Authorization", "Bearer " + bearerToken);
    }

    return httpClient.send(
        request.build(),
        HttpResponse.BodyHandlers.ofString()
    );
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  /*
   * TEST JWT ENCODER
   */

  @TestConfiguration(proxyBeanMethods = false)
  static class JwtTestConfiguration {

    @Bean
    @Primary
    JwtEncoder testJwtEncoder() throws Exception {
      var publicKey = RsaKeyConverters.x509().convert(
          new ClassPathResource("keys/public.pem")
              .getInputStream()
      );

      var privateKey = RsaKeyConverters.pkcs8().convert(
          new ClassPathResource("keys/private.pem")
              .getInputStream()
      );

      RSAKey rsaKey = new RSAKey.Builder(publicKey)
          .privateKey(privateKey)
          .keyID("gateway-test-key")
          .build();

      var jwkSource = new ImmutableJWKSet<SecurityContext>(
          new JWKSet(rsaKey)
      );

      return new NimbusJwtEncoder(jwkSource);
    }
  }
}