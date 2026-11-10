package org.course_project.order_service.security;

import org.course_project.order_service.entity.Order;
import org.course_project.order_service.repository.OrderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceAuthorizationTest {

  private OrderRepository orderRepository;
  private ResourceAuthorization authorization;

  @BeforeEach
  void setUp() {
    orderRepository = mock(OrderRepository.class);
    authorization = new ResourceAuthorization(orderRepository);
  }

  private JwtAuthenticationToken authentication(Long userId) {

    Jwt jwt = Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .claim("userId", userId)
        .claim("email", "user@example.com")
        .build();

    return new JwtAuthenticationToken(
        jwt,
        List.of(
            new SimpleGrantedAuthority("ROLE_USER")
        )
    );
  }

  @Test
  void userCanAccessOwnOrder() {

    Order order = new Order();
    order.setId(10L);
    order.setUserId(7L);

    when(orderRepository.findByIdAndDeletedFalse(10L))
        .thenReturn(Optional.of(order));

    assertTrue(
        authorization.ownsOrder(
            10L,
            authentication(7L)
        )
    );
  }

  @Test
  void userCannotAccessAnotherUsersOrder() {

    Order order = new Order();
    order.setId(10L);
    order.setUserId(8L);

    when(orderRepository.findByIdAndDeletedFalse(10L))
        .thenReturn(Optional.of(order));

    assertFalse(
        authorization.ownsOrder(
            10L,
            authentication(7L)
        )
    );
  }

  @Test
  void userCannotAccessDeletedOrder() {

    when(orderRepository.findByIdAndDeletedFalse(10L))
        .thenReturn(Optional.empty());

    assertFalse(
        authorization.ownsOrder(
            10L,
            authentication(7L)
        )
    );
  }

  @Test
  void userCanAccessOnlyOwnUserId() {

    assertTrue(
        authorization.isCurrentUser(
            7L,
            authentication(7L)
        )
    );

    assertFalse(
        authorization.isCurrentUser(
            8L,
            authentication(7L)
        )
    );
  }

  @Test
  void userCanAccessOnlyOwnEmail() {

    assertTrue(
        authorization.isCurrentUserEmail(
            "user@example.com",
            authentication(7L)
        )
    );

    assertFalse(
        authorization.isCurrentUserEmail(
            "another@example.com",
            authentication(7L)
        )
    );
  }
}
