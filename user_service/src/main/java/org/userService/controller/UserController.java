package org.userService.controller;
import org.userService.dto.user.UserCreateRequest;
import org.userService.dto.user.UserResponse;
import org.userService.dto.user.UserUpdateRequest;
import org.userService.dto.user.UserRegistrationRequest;
import org.userService.service.UserService;
import org.userService.dto.user.UserWithCardsResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  @PreAuthorize("permitAll()")
  public ResponseEntity<UserResponse> register(
      @Valid @RequestBody UserRegistrationRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userService.register(request));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> create(
      @Valid @RequestBody UserCreateRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userService.create(request));
  }

  @GetMapping("/{id}")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.isCurrentUser(#p0, authentication)"
  )
  public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getById(id));
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<UserResponse>> getAll(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String surname,
      @PageableDefault(
          sort = "id",
          direction = Sort.Direction.ASC
      ) Pageable pageable
  ) {
    return ResponseEntity.ok(
        userService.getAll(name, surname, pageable)
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.isCurrentUser(#p0, authentication)"
  )
  public ResponseEntity<UserResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody UserUpdateRequest request
  ) {
    return ResponseEntity.ok(userService.update(id, request));
  }

  @PatchMapping("/{id}/activate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> activate(@PathVariable Long id) {
    return ResponseEntity.ok(userService.activate(id));
  }

  @PatchMapping("/{id}/deactivate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> deactivate(@PathVariable Long id) {
    return ResponseEntity.ok(userService.deactivate(id));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/with-cards")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.isCurrentUser(#p0, authentication)"
  )
  public ResponseEntity<UserWithCardsResponse> getWithCards(
      @PathVariable Long id
  ) {
    return ResponseEntity.ok(userService.getWithCards(id));
  }

  @GetMapping("/active")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<java.util.List<UserResponse>> getActiveByName(
      @RequestParam String name
  ) {
    return ResponseEntity.ok(userService.getActiveByName(name));
  }

  @GetMapping("/without-cards")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<java.util.List<UserResponse>> getUsersWithoutCards() {
    return ResponseEntity.ok(userService.getUsersWithoutCards());
  }

  @GetMapping("/by-email")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> getByEmail(@RequestParam String email) {
    return ResponseEntity.ok(userService.getByEmail(email));
  }

}