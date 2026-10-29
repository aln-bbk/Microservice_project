package org.course_project.authenticationService.controller;

import jakarta.validation.Valid;
import org.course_project.authenticationService.dto.LoginRequest;
import org.course_project.authenticationService.dto.RefreshTokenRequest;
import org.course_project.authenticationService.dto.TokenResponse;
import org.course_project.authenticationService.dto.TokenValidationRequest;
import org.course_project.authenticationService.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationService authService;

  public AuthController(AuthenticationService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(
      @Valid @RequestBody LoginRequest request
  ) {
    return ResponseEntity.ok(authService.login(request));
  }


  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest request
  ) {
    return ResponseEntity.ok(
        authService.refresh(request.refreshToken())
    );
  }

  @PostMapping("/validate")
  public ResponseEntity<?> validate(
      @Valid @RequestBody TokenValidationRequest request
  ) {
    return ResponseEntity.ok(
        authService.validateToken(request.token())
    );
  }

}

