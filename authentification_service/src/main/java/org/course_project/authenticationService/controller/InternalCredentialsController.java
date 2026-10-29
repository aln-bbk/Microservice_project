package org.course_project.authenticationService.controller;

import jakarta.validation.Valid;
import org.course_project.authenticationService.dto.CreateCredentialsRequest;
import org.course_project.authenticationService.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/credentials")
public class InternalCredentialsController {

  private final AuthenticationService authenticationService;
  private final String internalApiKey;

  public InternalCredentialsController(
      AuthenticationService authenticationService,
      @Value("${app.internal-api.key:}") String internalApiKey
  ) {
    this.authenticationService = authenticationService;
    this.internalApiKey = internalApiKey;
  }

  @PostMapping
  public ResponseEntity<Void> createCredentials(
      @RequestHeader(
          value = "X-Internal-Api-Key",
          required = false
      ) String suppliedApiKey,
      @Valid @RequestBody CreateCredentialsRequest request
  ) {
    if (!isValidApiKey(suppliedApiKey)) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Unauthorized"
      );
    }

    authenticationService.createCredentials(request);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  private boolean isValidApiKey(String suppliedApiKey) {
    if (internalApiKey == null || internalApiKey.isBlank()
        || suppliedApiKey == null || suppliedApiKey.isBlank()) {
      return false;
    }

    return MessageDigest.isEqual(
        internalApiKey.getBytes(StandardCharsets.UTF_8),
        suppliedApiKey.getBytes(StandardCharsets.UTF_8)
    );
  }
}