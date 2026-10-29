package org.course_project.authenticationService.security;

import org.course_project.authenticationService.entity.Credential;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final String issuer;
  private final long accessTokenMinutes;
  private final long refreshTokenDays;

  public JwtService(
      JwtEncoder jwtEncoder,
      @Qualifier("jwtTokenDecoder") JwtDecoder jwtDecoder,
      @Value("${app.jwt.issuer}") String issuer,
      @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes,
      @Value("${app.jwt.refresh-token-days}") long refreshTokenDays
  ) {
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.issuer = issuer;
    this.accessTokenMinutes = accessTokenMinutes;
    this.refreshTokenDays = refreshTokenDays;
  }

  public String generateAccessToken(Credential credential) {
    Instant now = Instant.now();

    return generateToken(
        credential,
        "access",
        now,
        now.plus(accessTokenMinutes, ChronoUnit.MINUTES)
    );
  }

  public String generateRefreshToken(Credential credential) {
    Instant now = Instant.now();

    return generateToken(
        credential,
        "refresh",
        now,
        now.plus(refreshTokenDays, ChronoUnit.DAYS)
    );
  }

  private String generateToken(
      Credential credential,
      String tokenType,
      Instant issuedAt,
      Instant expiresAt
  ) {
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .subject(credential.getUserId().toString())
        .id(UUID.randomUUID().toString())
        .claim("userId", credential.getUserId())
        .claim("role", credential.getRole().name())
        .claim("tokenType", tokenType)
        .build();

    JwsHeader header = JwsHeader
        .with(SignatureAlgorithm.RS256)
        .build();

    return jwtEncoder.encode(
        JwtEncoderParameters.from(header, claims)
    ).getTokenValue();
  }

  public Jwt decodeAndValidate(String token, String expectedType) {
    Jwt jwt = jwtDecoder.decode(token);

    String tokenIssuer = jwt.getClaimAsString("iss");

    if (!issuer.equals(tokenIssuer)) {
      throw new JwtException("Invalid token issuer");
    }

    String tokenType = jwt.getClaimAsString("tokenType");

    if (!expectedType.equals(tokenType)) {
      throw new JwtException("Invalid token type");
    }

    if (jwt.getSubject() == null || jwt.getClaim("userId") == null
        || jwt.getClaimAsString("role") == null) {
      throw new JwtException("Required claims are missing");
    }

    String role = jwt.getClaimAsString("role");
    if (!"ADMIN".equals(role) && !"USER".equals(role)) {
      throw new JwtException("Invalid role claim");
    }

    try {
      Long subjectUserId = Long.valueOf(jwt.getSubject());
      Object userIdClaim = jwt.getClaim("userId");

      if (!(userIdClaim instanceof Number number)
          || subjectUserId.longValue() != number.longValue()) {
        throw new JwtException("Inconsistent user ID claims");
      }
    } catch (NumberFormatException exception) {
      throw new JwtException("Invalid subject claim", exception);
    }

    return jwt;
  }

  public String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(
          token.getBytes(StandardCharsets.UTF_8)
      );
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(
          "SHA-256 algorithm is unavailable",
          exception
      );
    }
  }
}
