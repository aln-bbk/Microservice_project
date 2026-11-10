package org.course_project.order_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.course_project.order_service.dto.CreateOrderRequest;
import org.course_project.order_service.dto.OrderResponse;
import org.course_project.order_service.dto.UpdateOrderRequest;
import org.course_project.order_service.entity.OrderStatus;
import org.course_project.order_service.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.isCurrentUserEmail(" +
          "authentication.token.claims['email'], authentication)"
  )
  public ResponseEntity<OrderResponse> createOrder(
      @Valid @RequestBody CreateOrderRequest request,
      @AuthenticationPrincipal Jwt jwt
  ) {

    Long userId = extractUserId(jwt);
    String email = jwt.getClaimAsString("email");

    if (email == null || email.isBlank()) {
      throw new AccessDeniedException("Email claim is missing in JWT");
    }

    OrderResponse response = orderService.createOrder(request, userId, email);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }


  @GetMapping("/{id}")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.ownsOrder(#id, authentication)"
  )
  public ResponseEntity<OrderResponse> getOrderById(
      @PathVariable("id") @P("id") Long id
  ) {

    return ResponseEntity.ok(orderService.getOrderById(id));
  }


  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<OrderResponse>> getOrders(

      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      Instant from,

      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      Instant to,

      @RequestParam(required = false)
      Set<OrderStatus> statuses,

      @PageableDefault(
          size = 10,
          sort = "createdAt",
          direction = Sort.Direction.DESC
      )
      Pageable pageable
  ) {

    return ResponseEntity.ok(
        orderService.getOrders(
            from,
            to,
            statuses,
            pageable
        )
    );
  }


  @GetMapping("/user/{userId}")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.isCurrentUser(#userId, authentication)"
  )
  public ResponseEntity<Page<OrderResponse>> getOrdersByUserId(

      @PathVariable("userId") @P("userId") Long userId,

      @PageableDefault(
          size = 10,
          sort = "createdAt",
          direction = Sort.Direction.DESC
      )
      Pageable pageable
  ) {

    return ResponseEntity.ok(orderService.getOrdersByUserId(userId, pageable));
  }


  @PutMapping("/{id}")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.ownsOrder(#id, authentication)"
  )
  public ResponseEntity<OrderResponse> updateOrder(

      @PathVariable("id") @P("id") Long id,

      @Valid @RequestBody UpdateOrderRequest request
  ) {

    return ResponseEntity.ok(orderService.updateOrder(id, request));
  }


  @DeleteMapping("/{id}")
  @PreAuthorize(
      "hasRole('ADMIN') or " +
          "@resourceAuthorization.ownsOrder(#id, authentication)"
  )
  public ResponseEntity<Void> deleteOrder(
      @PathVariable("id") @P("id") Long id
  ) {

    orderService.deleteOrder(id);

    return ResponseEntity.noContent().build();
  }

  private Long extractUserId(Jwt jwt) {

    if (jwt == null) {
      throw new AccessDeniedException("Authentication is required");
    }

    Object claim = jwt.getClaim("userId");

    if (claim == null) {
      throw new AccessDeniedException("User ID claim is missing in JWT");
    }

    try {
      return Long.valueOf(claim.toString());
    } catch (NumberFormatException ex) {
      throw new AccessDeniedException("Invalid user ID claim in JWT");
    }
  }
}
