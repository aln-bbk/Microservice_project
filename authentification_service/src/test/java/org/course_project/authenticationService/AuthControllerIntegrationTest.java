package org.course_project.authenticationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.course_project.authenticationService.entity.Credential;
import org.course_project.authenticationService.entity.RefreshToken;
import org.course_project.authenticationService.repository.CredentialRepository;
import org.course_project.authenticationService.repository.RefreshTokenRepository;
import org.course_project.authenticationService.security.JwtService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.internal-api.key=test-internal-api-key",
    "app.jwt.issuer=authentication-service-test",
    "app.jwt.access-token-minutes=15",
    "app.jwt.refresh-token-days=7",
    "app.bootstrap-admin.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIntegrationTest {

  private static Path publicKeyPath;
  private static Path privateKeyPath;

  @DynamicPropertySource
  static void configureJwtKeys(DynamicPropertyRegistry registry) throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();

    publicKeyPath = Files.createTempFile("auth-test-public-", ".pem");
    privateKeyPath = Files.createTempFile("auth-test-private-", ".pem");

    Files.writeString(
        publicKeyPath,
        pem("PUBLIC KEY", pair.getPublic().getEncoded())
    );
    Files.writeString(
        privateKeyPath,
        pem("PRIVATE KEY", pair.getPrivate().getEncoded())
    );

    registry.add("app.jwt.public-key-location", publicKeyPath::toString);
    registry.add("app.jwt.private-key-location", privateKeyPath::toString);
  }

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  CredentialRepository credentialRepository;

  @Autowired
  RefreshTokenRepository refreshTokenRepository;

  @Autowired
  JwtService jwtService;

  @Autowired
  org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

  private String login;
  private String password;
  private long userId;

  @BeforeEach
  void setUp() {
    login = "auth-test-" + UUID.randomUUID();
    password = "StrongPassword123!";
    userId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;

    if (userId == 0) {
      userId = 987654321L;
    }

    mockCreateCredentials(userId, login, password);
  }

  @Test
  void loginReturnsAccessAndRefreshTokensWithExpectedClaims() throws Exception {
    JsonNode response = loginAndReadTokens(login, password);

    String accessToken = response.get("accessToken").asText();
    String refreshToken = response.get("refreshToken").asText();

    assertThat(response.get("tokenType").asText()).isEqualTo("Bearer");
    assertThat(accessToken).isNotBlank();
    assertThat(refreshToken).isNotBlank();

    JwtDecoder decoder = NimbusJwtDecoder
        .withPublicKey(readPublicKey())
        .build();

    Jwt access = decoder.decode(accessToken);
    Jwt refresh = decoder.decode(refreshToken);

    assertThat(access.getClaimAsString("iss"))
        .isEqualTo("authentication-service-test");
    assertThat(access.getSubject()).isEqualTo(Long.toString(userId));
    assertThat((Number) access.getClaim("userId")).isNotNull();
    assertThat(((Number) access.getClaim("userId")).longValue())
        .isEqualTo(userId);
    assertThat(access.getClaimAsString("role")).isEqualTo("USER");
    assertThat(access.getClaimAsString("tokenType")).isEqualTo("access");
    assertThat(access.getExpiresAt()).isAfter(access.getIssuedAt());

    assertThat(refresh.getClaimAsString("iss"))
        .isEqualTo("authentication-service-test");
    assertThat(refresh.getSubject()).isEqualTo(Long.toString(userId));

    assertThat((Number) refresh.getClaim("userId")).isNotNull();
    assertThat(((Number) refresh.getClaim("userId")).longValue())
        .isEqualTo(userId);
    assertThat(refresh.getClaimAsString("role")).isEqualTo("USER");
    assertThat(refresh.getClaimAsString("tokenType")).isEqualTo("refresh");
    assertThat(refresh.getExpiresAt()).isAfter(refresh.getIssuedAt());

    String expectedHash = jwtService.hashToken(refreshToken);

    RefreshToken stored = refreshTokenRepository
        .findByTokenHash(expectedHash)
        .orElseThrow();

    assertThat(stored.getUserId()).isEqualTo(userId);
    assertThat(stored.getTokenHash()).hasSize(64).isEqualTo(expectedHash);
    assertThat(stored.isRevoked()).isFalse();

    assertThat(refreshTokenRepository.findAll())
        .noneMatch(token -> token.getTokenHash().equals(refreshToken));
  }

  @Test
  void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson(login, "WrongPassword123!")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithUnknownLoginReturnsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson("unknown-" + UUID.randomUUID(), password)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshRotatesTokenAndOldTokenCannotBeReused() throws Exception {
    JsonNode initial = loginAndReadTokens(login, password);
    String oldRefresh = initial.get("refreshToken").asText();

    MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshJson(oldRefresh)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andReturn();

    JsonNode rotated = objectMapper.readTree(
        refreshResult.getResponse().getContentAsString()
    );

    String newRefresh = rotated.get("refreshToken").asText();

    assertThat(newRefresh).isNotEqualTo(oldRefresh);

    RefreshToken oldStored = refreshTokenRepository
        .findByTokenHash(jwtService.hashToken(oldRefresh))
        .orElseThrow();

    assertThat(oldStored.isRevoked()).isTrue();

    assertThat(refreshTokenRepository.findByTokenHash(
        jwtService.hashToken(newRefresh)
    )).isPresent();

    // Повторное использование старого refresh-токена запрещено.
    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshJson(oldRefresh)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void validateAcceptsAccessTokenAndRejectsRefreshToken() throws Exception {
    JsonNode tokens = loginAndReadTokens(login, password);

    // Access-токен должен пройти проверку.
    mockMvc.perform(post("/api/auth/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(tokenJson(tokens.get("accessToken").asText())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.tokenType").value("access"))
        .andExpect(jsonPath("$.userId").value(Long.toString(userId)))
        .andExpect(jsonPath("$.role").value("USER"));

    // Refresh-токен не должен приниматься как access-токен.
    mockMvc.perform(post("/api/auth/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(tokenJson(tokens.get("refreshToken").asText())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(false));
  }

  @Test
  void validateReturnsInvalidForMalformedToken() throws Exception {
    mockMvc.perform(post("/api/auth/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(tokenJson("not-a-jwt")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(false));
  }

  @Test
  void bcryptUsesDifferentSaltForTheSamePassword() {
    long secondUserId = userId == Long.MAX_VALUE ? userId - 1 : userId + 1;
    String secondLogin = "auth-salt-test-" + UUID.randomUUID();
    mockCreateCredentials(secondUserId, secondLogin, password);

    Credential first = credentialRepository.findByLogin(login).orElseThrow();
    Credential second = credentialRepository.findByLogin(secondLogin).orElseThrow();

    assertThat(first.getPasswordHash()).isNotEqualTo(second.getPasswordHash());
    assertThat(passwordEncoder.matches(password, first.getPasswordHash())).isTrue();
    assertThat(passwordEncoder.matches(password, second.getPasswordHash())).isTrue();
  }

  @Test
  void credentialsAreStoredWithEncodedPassword() {
    Credential credential = credentialRepository
        .findByLogin(login)
        .orElseThrow();

    assertThat(credential.getPasswordHash()).isNotEqualTo(password);

    assertThat(passwordEncoder.matches(
        password,
        credential.getPasswordHash()
    )).isTrue();
  }

  private JsonNode loginAndReadTokens(String login, String password)
      throws Exception {

    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson(login, password)))
        .andExpect(status().isOk())
        .andReturn();

    return objectMapper.readTree(
        result.getResponse().getContentAsString()
    );
  }

  private void mockCreateCredentials(long id, String login, String password) {
    try {
      mockMvc.perform(post("/internal/credentials")
              .header("X-Internal-Api-Key", "test-internal-api-key")
              .contentType(MediaType.APPLICATION_JSON)
              .content(credentialsJson(id, login, password)))
          .andExpect(status().isCreated());
    } catch (Exception exception) {
      throw new AssertionError(
          "Could not create test credentials",
          exception
      );
    }
  }

  private String loginJson(String login, String password) {
    return """
        {"login":"%s","password":"%s"}
        """.formatted(login, password);
  }

  private String credentialsJson(long id, String login, String password) {
    return """
        {"userId":%d,"login":"%s","password":"%s"}
        """.formatted(id, login, password);
  }

  private String refreshJson(String token) {
    return """
        {"refreshToken":"%s"}
        """.formatted(token);
  }

  private String tokenJson(String token) {
    return """
        {"token":"%s"}
        """.formatted(token);
  }

  private RSAPublicKey readPublicKey() throws Exception {
    try (var input = Files.newInputStream(publicKeyPath)) {
      return (RSAPublicKey) org.springframework.security.converter
          .RsaKeyConverters.x509()
          .convert(input);
    }
  }

  private static String pem(String label, byte[] encoded) {
    return "-----BEGIN " + label + "-----\n"
        + Base64.getMimeEncoder(64, new byte[]{'\n'})
        .encodeToString(encoded)
        + "\n-----END " + label + "-----\n";
  }
}
