package org.course_project.order_service.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.course_project.order_service.dto.*;
import org.course_project.order_service.entity.OrderStatus;
import org.course_project.order_service.exception.NotFoundException;
import org.course_project.order_service.repository.ItemRepository;
import org.course_project.order_service.repository.OrderRepository;
import org.course_project.order_service.service.ItemService;
import org.course_project.order_service.service.OrderService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

  private static final Long USER_ID = 71L;
  private static final String USER_EMAIL = "integration@example.com";
  private static final String TOKEN = "integration-test-token";

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withCommand("postgres", "-c", "timezone=UTC");

  static final WireMockServer userService = new WireMockServer(0);

  static {
    userService.start();
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add(
        "spring.datasource.driver-class-name",
        () -> "org.postgresql.Driver"
    );

    registry.add("user-service.url", userService::baseUrl);

    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.liquibase.enabled", () -> "true");
    registry.add(
        "spring.jpa.properties.hibernate.jdbc.time_zone",
        () -> "UTC"
    );
  }

  @Autowired
  ItemService itemService;

  @Autowired
  OrderService orderService;

  @Autowired
  ItemRepository itemRepository;

  @Autowired
  OrderRepository orderRepository;

  @AfterAll
  static void stopWireMock() {
    userService.stop();
  }

  @AfterEach
  void cleanUp() {
    SecurityContextHolder.clearContext();

    orderRepository.deleteAll();
    itemRepository.deleteAll();

    userService.resetAll();
  }


  private void authenticateAsUser() {
    Jwt jwt = Jwt.withTokenValue(TOKEN)
        .header("alg", "none")
        .claim("sub", String.valueOf(USER_ID))
        .claim("userId", USER_ID)
        .claim("email", USER_EMAIL)
        .claim("role", "USER")
        .claim("tokenType", "access")
        .build();

    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            jwt,
            List.of(
                new SimpleGrantedAuthority("ROLE_USER")
            )
        );

    SecurityContextHolder.setContext(
        SecurityContextHolder.createEmptyContext()
    );

    SecurityContextHolder.getContext()
        .setAuthentication(authentication);
  }


  private void stubUserByEmail() {
    userService.stubFor(
        get(urlPathEqualTo("/api/users/by-email"))
            .withQueryParam("email", equalTo(USER_EMAIL))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(
                    HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_JSON_VALUE
                )
                .withBody(userJson()))
    );
  }

  private void stubUserById(Long id) {
    userService.stubFor(
        get(urlEqualTo("/api/users/" + id))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(
                    HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_JSON_VALUE
                )
                .withBody(userJson()))
    );
  }

  private void stubUsersByIds() {
    userService.stubFor(
        post(urlEqualTo("/api/users/by-ids"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(
                    HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_JSON_VALUE
                )
                .withBody("[" + userJson() + "]"))
    );
  }

  private String userJson() {
    return """
                {
                    "id": 71,
                    "email": "integration@example.com",
                    "name": "Integration",
                    "surname": "User"
                }
                """;
  }

  private void stubAllUserEndpoints() {
    stubUserByEmail();
    stubUserById(USER_ID);
    stubUsersByIds();
  }


  @Test
  void itemCrudPersistsDataInPostgres() {
    ItemResponse created = itemService.create(
        new ItemCreateRequest(
            "Monitor",
            new BigDecimal("199.90")
        )
    );

    assertNotNull(created.id());

    assertEquals(
        "Monitor",
        itemRepository.findById(created.id())
            .orElseThrow()
            .getName()
    );

    ItemResponse updated = itemService.update(
        created.id(),
        new ItemUpdateRequest(
            "4K Monitor",
            new BigDecimal("249.90")
        )
    );

    assertEquals("4K Monitor", updated.name());

    assertEquals(
        1,
        itemService.getAll(PageRequest.of(0, 10))
            .getTotalElements()
    );

    itemService.delete(created.id());

    assertFalse(itemRepository.existsById(created.id()));
  }


  @Test
  void createAndReadOrderPersistsLinesAndCallsUserService() {
    authenticateAsUser();
    stubAllUserEndpoints();

    ItemResponse product = itemService.create(
        new ItemCreateRequest(
            "SSD",
            new BigDecimal("80.00")
        )
    );

    OrderResponse created = orderService.createOrder(
        new CreateOrderRequest(List.of(
            new CreateOrderRequest.OrderItemRequest(
                product.id(),
                2
            )
        )),
        USER_ID,
        USER_EMAIL
    );

    assertNotNull(created.id());
    assertEquals(USER_ID, created.userId());
    assertEquals(USER_EMAIL, created.user().email());
    assertEquals(new BigDecimal("160.00"), created.totalPrice());
    assertEquals(1, created.items().size());

    OrderResponse loaded =
        orderService.getOrderById(created.id());

    assertEquals(created.id(), loaded.id());
    assertEquals(USER_EMAIL, loaded.user().email());

    Page<OrderResponse> allOrders = orderService.getOrders(
        null,
        null,
        Set.of(OrderStatus.CREATED),
        PageRequest.of(0, 10)
    );

    assertEquals(1, allOrders.getTotalElements());

    Page<OrderResponse> userOrders =
        orderService.getOrdersByUserId(
            USER_ID,
            PageRequest.of(0, 10)
        );

    assertEquals(1, userOrders.getTotalElements());

    userService.verify(
        getRequestedFor(urlPathEqualTo("/api/users/by-email"))
            .withQueryParam("email", equalTo(USER_EMAIL))
            .withHeader(
                HttpHeaders.AUTHORIZATION,
                equalTo("Bearer " + TOKEN)
            )
    );

    userService.verify(
        postRequestedFor(urlEqualTo("/api/users/by-ids"))
            .withHeader(
                HttpHeaders.AUTHORIZATION,
                equalTo("Bearer " + TOKEN)
            )
    );
  }

  @Test
  void updateOrderReplacesItemsAndSoftDeleteHidesOrder() {
    authenticateAsUser();
    stubAllUserEndpoints();

    ItemResponse first = itemService.create(
        new ItemCreateRequest(
            "First",
            new BigDecimal("10.00")
        )
    );

    ItemResponse second = itemService.create(
        new ItemCreateRequest(
            "Second",
            new BigDecimal("15.00")
        )
    );

    OrderResponse created = orderService.createOrder(
        new CreateOrderRequest(List.of(
            new CreateOrderRequest.OrderItemRequest(
                first.id(),
                1
            )
        )),
        USER_ID,
        USER_EMAIL
    );

    OrderResponse updated = orderService.updateOrder(
        created.id(),
        new UpdateOrderRequest(
            OrderStatus.PROCESSING,
            List.of(
                new CreateOrderRequest.OrderItemRequest(
                    second.id(),
                    3
                )
            )
        )
    );

    assertEquals(OrderStatus.PROCESSING, updated.status());
    assertEquals(new BigDecimal("45.00"), updated.totalPrice());
    assertEquals(second.id(), updated.items().getFirst().itemId());

    orderService.deleteOrder(created.id());

    assertTrue(
        orderRepository.findById(created.id())
            .orElseThrow()
            .isDeleted()
    );

    assertThrows(
        NotFoundException.class,
        () -> orderService.getOrderById(created.id())
    );
  }

  @Test
  void deletingItemReferencedByOrderReturnsConflict() {
    authenticateAsUser();
    stubAllUserEndpoints();

    ItemResponse product = itemService.create(
        new ItemCreateRequest(
            "Referenced",
            new BigDecimal("5.00")
        )
    );

    orderService.createOrder(
        new CreateOrderRequest(List.of(
            new CreateOrderRequest.OrderItemRequest(
                product.id(),
                1
            )
        )),
        USER_ID,
        USER_EMAIL
    );

    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> itemService.delete(product.id())
    );
  }
}