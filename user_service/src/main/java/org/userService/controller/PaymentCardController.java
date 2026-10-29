package org.userService.controller;

import org.userService.dto.card.PaymentCardCreateRequest;
import org.userService.dto.card.PaymentCardResponse;
import org.userService.dto.card.PaymentCardUpdateRequest;
import org.userService.service.PaymentCardService;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class PaymentCardController {

  private final PaymentCardService cardService;

  public PaymentCardController(PaymentCardService cardService) {
    this.cardService = cardService;
  }

  @PostMapping
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.isCurrentUser(#p0.userId, authentication)"
  )
  public ResponseEntity<PaymentCardResponse> create(
      @Valid @RequestBody PaymentCardCreateRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(cardService.create(request));
  }

  @GetMapping("/{id}")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.ownsCard(#p0, authentication)"
  )
  public ResponseEntity<PaymentCardResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(cardService.getById(id));
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<PaymentCardResponse>> getAll(
      @PageableDefault(
          sort = "id",
          direction = Sort.Direction.ASC
      ) Pageable pageable
  ) {
    return ResponseEntity.ok(cardService.getAll(pageable));
  }

  @GetMapping("/user/{userId}")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.isCurrentUser(#p0, authentication)"
  )
  public ResponseEntity<List<PaymentCardResponse>> getByUserId(@PathVariable Long userId) {
    return ResponseEntity.ok(cardService.getByUserId(userId));
  }

  @PutMapping("/{id}")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.ownsCard(#p0, authentication)"
  )
  public ResponseEntity<PaymentCardResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody PaymentCardUpdateRequest request
  ) {
    return ResponseEntity.ok(cardService.update(id, request));
  }

  @PatchMapping("/{id}/activate")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.ownsCard(#p0, authentication)"
  )
  public ResponseEntity<PaymentCardResponse> activate(@PathVariable Long id) {
    return ResponseEntity.ok(cardService.activate(id));
  }

  @PatchMapping("/{id}/deactivate")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.ownsCard(#p0, authentication)"
  )
  public ResponseEntity<PaymentCardResponse> deactivate(@PathVariable Long id) {
    return ResponseEntity.ok(cardService.deactivate(id));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.ownsCard(#p0, authentication)"
  )
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    cardService.delete(id);
    return ResponseEntity.noContent().build();
  }
}