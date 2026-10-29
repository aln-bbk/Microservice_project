package org.userService.integration;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserServiceSecurityIntegrationTest extends AbstractIntegrationTest {

  private static final String ISSUER = "authentication-service";

  private static KeyPair testKeyPair;
  private static KeyPair otherKeyPair;
  private static Path publicKeyFile;

  @Autowired
  MockMvc mockMvc;

  /*
   * Spring invokes DynamicPropertySource before creating application beans.
   * Generate the keys and write the public key before JwtConfig loads it.
   */
  @DynamicPropertySource
  static void configureJwtProperties(DynamicPropertyRegistry registry) {
    try {
      if (testKeyPair == null) {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        testKeyPair = generator.generateKeyPair();
        otherKeyPair = generator.generateKeyPair();

        publicKeyFile = Files.createTempFile(
            "user-service-test-public-",
            ".pem"
        );

        String pem = "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'})
            .encodeToString(testKeyPair.getPublic().getEncoded())
            + "\n-----END PUBLIC KEY-----\n";

        Files.writeString(publicKeyFile, pem);
      }

      registry.add(
          "app.jwt.public-key-location",
          () -> publicKeyFile.toUri().toString()
      );
      registry.add("app.jwt.issuer", () -> ISSUER);

    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to generate test RSA keys",
          e
      );
    }
  }

  @Test
  void requestWithoutTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/users/999999999"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void tokenSignedWithWrongKeyReturnsUnauthorized() throws Exception {
    String token = createToken(
        otherKeyPair,
        "access",
        Instant.now().plusSeconds(300),
        ISSUER
    );

    mockMvc.perform(get("/api/users/999999999")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expiredTokenReturnsUnauthorized() throws Exception {
    String token = createToken(
        testKeyPair,
        "access",
        Instant.now().minusSeconds(60),
        ISSUER
    );

    mockMvc.perform(get("/api/users/999999999")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshTokenReturnsUnauthorized() throws Exception {
    String token = createToken(
        testKeyPair,
        "refresh",
        Instant.now().plusSeconds(300),
        ISSUER
    );

    mockMvc.perform(get("/api/users/999999999")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void wrongIssuerReturnsUnauthorized() throws Exception {
    String token = createToken(
        testKeyPair,
        "access",
        Instant.now().plusSeconds(300),
        "some-other-issuer"
    );

    mockMvc.perform(get("/api/users/999999999")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void validAccessTokenPassesAuthentication() throws Exception {
    String token = createToken(
        testKeyPair,
        "access",
        Instant.now().plusSeconds(300),
        ISSUER
    );

    mockMvc.perform(get("/api/users/999999999")
            .header("Authorization", "Bearer " + token)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  private String createToken(
      KeyPair keyPair,
      String tokenType,
      Instant expiresAt,
      String issuer
  ) {
    Instant now = Instant.now();

    return Jwts.builder()
        .issuer(issuer)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .claim("tokenType", tokenType)
        .claim("role", "ADMIN")
        .claim("userId", 1L)
        .signWith((RSAPrivateKey) keyPair.getPrivate())
        .compact();
  }
}
