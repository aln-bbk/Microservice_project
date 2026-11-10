package org.course_project.order_service.integration;

import org.course_project.order_service.controller.OrderController;
import org.course_project.order_service.dto.CreateOrderRequest;
import org.course_project.order_service.dto.UpdateOrderRequest;
import org.course_project.order_service.dto.OrderResponse;
import org.course_project.order_service.entity.Order;
import org.course_project.order_service.entity.OrderStatus;
import org.course_project.order_service.repository.OrderRepository;
import org.course_project.order_service.security.ResourceAuthorization;
import org.course_project.order_service.service.OrderService;
import org.course_project.order_service.config.SecurityConfig;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({
    SecurityConfig.class,
    ResourceAuthorization.class
})
class OrderControllerIntegrationTest {

  private static final Long USER_ID = 71L;
  private static final Long OTHER_USER_ID = 99L;
  private static final Long ORDER_ID = 10L;
  private static final String USER_EMAIL = "user@example.com";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private OrderService orderService;

  @MockitoBean
  private OrderRepository orderRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;


  @Test
  void createOrderAsAuthenticatedUserReturns201() throws Exception {

    when(orderService.createOrder(
        any(CreateOrderRequest.class),
        eq(USER_ID),
        eq(USER_EMAIL)
    )).thenReturn(null);

    mockMvc.perform(post("/api/orders")
            .with(userJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                                {
                                  "items": [
                                    {
                                      "itemId": 1,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
        .andExpect(status().isCreated());

    verify(orderService).createOrder(
        any(CreateOrderRequest.class),
        eq(USER_ID),
        eq(USER_EMAIL)
    );
  }

  @Test
  void createOrderWithEmptyItemsReturns400() throws Exception {

    mockMvc.perform(post("/api/orders")
            .with(userJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                                {
                                  "items": []
                                }
                                """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(orderService);
  }

  @Test
  void createOrderWithInvalidQuantityReturns400() throws Exception {

    mockMvc.perform(post("/api/orders")
            .with(userJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                                {
                                  "items": [
                                    {
                                      "itemId": 1,
                                      "quantity": 0
                                    }
                                  ]
                                }
                                """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(orderService);
  }


  @Test
  void getOwnOrderReturns200() throws Exception {

    mockOwnOrder();

    when(orderService.getOrderById(ORDER_ID))
        .thenReturn(null);

    mockMvc.perform(get("/api/orders/{id}", ORDER_ID)
            .with(userJwt()))
        .andExpect(status().isOk());

    verify(orderService).getOrderById(ORDER_ID);
  }

  @Test
  void getOtherUsersOrderReturns403() throws Exception {

    mockOtherUsersOrder();

    mockMvc.perform(get("/api/orders/{id}", ORDER_ID)
            .with(userJwt()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }

  @Test
  void getOrderWithoutAuthenticationReturns401() throws Exception {

    mockMvc.perform(get("/api/orders/{id}", ORDER_ID))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(orderService);
  }

  @Test
  void adminCanGetOtherUsersOrder() throws Exception {

    when(orderService.getOrderById(ORDER_ID))
        .thenReturn(null);

    mockMvc.perform(get("/api/orders/{id}", ORDER_ID)
            .with(adminJwt()))
        .andExpect(status().isOk());

    verify(orderService).getOrderById(ORDER_ID);
  }


  @Test
  void adminCanGetAllOrders() throws Exception {

    when(orderService.getOrders(
        any(),
        any(),
        any(),
        any()
    )).thenReturn(null);

    mockMvc.perform(get("/api/orders")
            .with(adminJwt()))
        .andExpect(status().isOk());

    verify(orderService).getOrders(
        any(),
        any(),
        any(),
        any()
    );
  }

  @Test
  void regularUserCannotGetAllOrders() throws Exception {

    mockMvc.perform(get("/api/orders")
            .with(userJwt()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }

  @Test
  void adminCanFilterOrdersByStatus() throws Exception {

    when(orderService.getOrders(
        any(),
        any(),
        any(),
        any()
    )).thenReturn(null);

    mockMvc.perform(get("/api/orders")
            .with(adminJwt())
            .param("statuses", "CREATED", "COMPLETED"))
        .andExpect(status().isOk());

    verify(orderService).getOrders(
        any(),
        any(),
        argThat(statuses ->
            statuses != null
                && statuses.containsAll(List.of(
                OrderStatus.CREATED,
                OrderStatus.COMPLETED
            ))
        ),
        any()
    );
  }


  @Test
  void userCanGetOwnOrders() throws Exception {

    when(orderService.getOrdersByUserId(
        eq(USER_ID),
        any(Pageable.class)
    )).thenReturn(null);

    mockMvc.perform(get("/api/orders/user/{userId}", USER_ID)
            .with(userJwt()))
        .andExpect(status().isOk());

    verify(orderService).getOrdersByUserId(eq(USER_ID), any());
  }

  @Test
  void userCannotGetOtherUsersOrders() throws Exception {

    mockMvc.perform(get("/api/orders/user/{userId}", OTHER_USER_ID)
            .with(userJwt()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }

  @Test
  void adminCanGetOtherUsersOrders() throws Exception {

    when(orderService.getOrdersByUserId(
        eq(OTHER_USER_ID),
        any(Pageable.class)
    )).thenReturn(null);

    mockMvc.perform(get("/api/orders/user/{userId}", OTHER_USER_ID)
            .with(adminJwt()))
        .andExpect(status().isOk());

    verify(orderService).getOrdersByUserId(eq(OTHER_USER_ID), any());
  }


  @Test
  void userCanUpdateOwnOrder() throws Exception {

    mockOwnOrder();

    when(orderService.updateOrder(
        eq(ORDER_ID),
        any(UpdateOrderRequest.class)
    )).thenReturn(null);

    mockMvc.perform(put("/api/orders/{id}", ORDER_ID)
            .with(userJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                                {
                                  "status": "CREATED",
                                  "items": [
                                    {
                                      "itemId": 1,
                                      "quantity": 3
                                    }
                                  ]
                                }
                                """))
        .andExpect(status().isOk());

    verify(orderService).updateOrder(
        eq(ORDER_ID),
        any(UpdateOrderRequest.class)
    );
  }

  @Test
  void userCannotUpdateOtherUsersOrder() throws Exception {

    mockOtherUsersOrder();

    mockMvc.perform(put("/api/orders/{id}", ORDER_ID)
            .with(userJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                                {
                                  "status": "CREATED",
                                  "items": [
                                    {
                                      "itemId": 1,
                                      "quantity": 3
                                    }
                                  ]
                                }
                                """))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }

  @Test
  void updateOrderWithoutStatusReturns400() throws Exception {

    mockOwnOrder();

    mockMvc.perform(put("/api/orders/{id}", ORDER_ID)
            .with(userJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                                {
                                  "items": [
                                    {
                                      "itemId": 1,
                                      "quantity": 3
                                    }
                                  ]
                                }
                                """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(orderService);
  }


  @Test
  void userCanDeleteOwnOrder() throws Exception {

    mockOwnOrder();

    doNothing().when(orderService).deleteOrder(ORDER_ID);

    mockMvc.perform(delete("/api/orders/{id}", ORDER_ID)
            .with(userJwt()))
        .andExpect(status().isNoContent());

    verify(orderService).deleteOrder(ORDER_ID);
  }

  @Test
  void userCannotDeleteOtherUsersOrder() throws Exception {

    mockOtherUsersOrder();

    mockMvc.perform(delete("/api/orders/{id}", ORDER_ID)
            .with(userJwt()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }


  private void mockOwnOrder() {

    Order order = mock(Order.class);

    when(order.getUserId()).thenReturn(USER_ID);

    when(orderRepository.findByIdAndDeletedFalse(ORDER_ID))
        .thenReturn(java.util.Optional.of(order));
  }

  private void mockOtherUsersOrder() {

    Order order = mock(Order.class);

    when(order.getUserId()).thenReturn(OTHER_USER_ID);

    when(orderRepository.findByIdAndDeletedFalse(ORDER_ID))
        .thenReturn(java.util.Optional.of(order));
  }


  private RequestPostProcessor userJwt() {

    return jwt()
        .jwt(jwt -> jwt
            .subject(USER_ID.toString())
            .claim("userId", USER_ID)
            .claim("email", USER_EMAIL)
            .claim("role", "USER")
            .claim("tokenType", "access")
            .claim("iss", "https://auth.example.com")
        )
        .authorities(
            () -> "ROLE_USER"
        );
  }

  private RequestPostProcessor adminJwt() {

    return jwt()
        .jwt(jwt -> jwt
            .subject(OTHER_USER_ID.toString())
            .claim("userId", OTHER_USER_ID)
            .claim("email", "admin@example.com")
            .claim("role", "ADMIN")
            .claim("tokenType", "access")
            .claim("iss", "https://auth.example.com")
        )
        .authorities(
            () -> "ROLE_ADMIN"
        );
  }
}