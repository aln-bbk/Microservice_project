package org.course_project.authenticationService.service;

import org.course_project.authenticationService.dto.LoginRequest;
import org.course_project.authenticationService.dto.TokenResponse;
import org.course_project.authenticationService.entity.Credential;
import org.course_project.authenticationService.entity.RefreshToken;
import org.course_project.authenticationService.repository.CredentialRepository;
import org.course_project.authenticationService.repository.RefreshTokenRepository;
import org.course_project.authenticationService.security.JwtService;
import org.course_project.authenticationService.dto.CreateCredentialsRequest;
import org.course_project.authenticationService.entity.Role;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Map;

@Service
public class AuthenticationService {

  private final CredentialRepository credentialRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtService jwtService;

  public AuthenticationService(
      CredentialRepository credentialRepository,
      PasswordEncoder passwordEncoder,
      RefreshTokenRepository refreshTokenRepository,
      JwtService jwtService
  ) {
    this.credentialRepository = credentialRepository;
    this.passwordEncoder = passwordEncoder;
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtService = jwtService;
  }

  @Transactional
  public TokenResponse login(LoginRequest request) {
    Credential credential = credentialRepository
        .findByLogin(request.login())
        .orElseThrow(() ->
            new BadCredentialsException("Invalid login or password")
        );

    if (!passwordEncoder.matches(
        request.password(),
        credential.getPasswordHash()
    )) {
      throw new BadCredentialsException("Invalid login or password");
    }

    return issueTokens(credential);
  }

  @Transactional
  public TokenResponse refresh(String rawRefreshToken) {
    Jwt jwt;

    try {
      jwt = jwtService.decodeAndValidate(rawRefreshToken, "refresh");
    } catch (JwtException exception) {
      throw new BadCredentialsException("Invalid refresh token");
    }

    String tokenHash = jwtService.hashToken(rawRefreshToken);

    RefreshToken storedToken = refreshTokenRepository
        .findByTokenHashForUpdate(tokenHash)
        .orElseThrow(() ->
            new BadCredentialsException("Invalid refresh token")
        );

    if (storedToken.isRevoked() || storedToken.isExpired()) {
      throw new BadCredentialsException("Invalid refresh token");
    }

    Long userId;
    try {
      userId = Long.valueOf(jwt.getSubject());
    } catch (NumberFormatException exception) {
      throw new BadCredentialsException("Invalid refresh token");
    }

    if (!userId.equals(storedToken.getUserId())) {
      throw new BadCredentialsException("Invalid refresh token");
    }

    Credential credential = credentialRepository
        .findByUserId(userId)
        .orElseThrow(() ->
            new BadCredentialsException("Invalid refresh token")
        );

    // Одноразовое использование refresh-токена.
    storedToken.revoke();
    refreshTokenRepository.save(storedToken);

    return issueTokens(credential);
  }

  private TokenResponse issueTokens(Credential credential) {
    String accessToken = jwtService.generateAccessToken(credential);
    String refreshToken = jwtService.generateRefreshToken(credential);

    Jwt refreshJwt;
    try {
      refreshJwt = jwtService.decodeAndValidate(refreshToken, "refresh");
    } catch (JwtException exception) {
      throw new IllegalStateException(
          "Could not validate newly generated refresh token",
          exception
      );
    }

    RefreshToken storedToken = new RefreshToken(
        credential.getUserId(),
        jwtService.hashToken(refreshToken),
        refreshJwt.getExpiresAt().atOffset(ZoneOffset.UTC)
    );

    refreshTokenRepository.save(storedToken);

    return new TokenResponse(
        accessToken,
        refreshToken,
        "Bearer"
    );
  }

  public Map<String, Object> validateToken(String token) {
    try {
      Jwt jwt = jwtService.decodeAndValidate(token, "access");

      return Map.of(
          "valid", true,
          "tokenType", "access",
          "userId", jwt.getSubject(),
          "role", jwt.getClaimAsString("role")
      );
    } catch (JwtException exception) {
      return Map.of("valid", false);
    }
  }

  @Transactional
  public void createCredentials(CreateCredentialsRequest request) {
    if (credentialRepository.existsByLogin(request.login())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Login already exists"
      );
    }

    if (credentialRepository.existsByUserId(request.userId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Credentials already exist for this user"
      );
    }

    int passwordBytes = request.password()
        .getBytes(StandardCharsets.UTF_8).length;

    if (passwordBytes > 72) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Password is too long"
      );
    }

    Credential credential = new Credential(
        request.userId(),
        request.login(),
        passwordEncoder.encode(request.password()),
        Role.USER
    );

    credentialRepository.save(credential);
  }
}
