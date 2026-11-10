package org.course_project.order_service.service;

import org.course_project.order_service.dto.*;
import org.course_project.order_service.entity.*;
import org.course_project.order_service.exception.NotFoundException;
import org.course_project.order_service.mapper.OrderMapper;
import org.course_project.order_service.repository.ItemRepository;
import org.course_project.order_service.repository.OrderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock
  OrderRepository orderRepository;

  @Mock
  ItemRepository itemRepository;

  @Mock
  OrderMapper orderMapper;

  @Mock
  UserServiceClient userServiceClient;

  @InjectMocks
  OrderService service;

  private UserInfoResponse user;
  private UserInfoResponse secondUser;

  private Item item;
  private Order order;
  private OrderResponse mappedResponse;

  @BeforeEach
  void setUp() {

    user = new UserInfoResponse(
        7L,
        "user@example.com",
        "Test",
        "User"
    );

    secondUser = new UserInfoResponse(
        8L,
        "second@example.com",
        "Second",
        "User"
    );

    item = new Item();
    item.setId(11L);
    item.setName("Book");
    item.setPrice(new BigDecimal("12.50"));

    order = new Order();
    order.setId(21L);
    order.setUserId(7L);
    order.setUserEmail("user@example.com");
    order.setStatus(OrderStatus.CREATED);
    order.setTotalPrice(new BigDecimal("25.00"));

    mappedResponse = new OrderResponse(
        21L,
        7L,
        OrderStatus.CREATED,
        new BigDecimal("25.00"),
        List.of(),
        null,
        null,
        null
    );
  }

  @Test
  void createOrderSetsAuthenticatedOwnerAndCalculatesTotal() {

    var line =
        new CreateOrderRequest.OrderItemRequest(11L, 2);

    var request =
        new CreateOrderRequest(List.of(line));

    OrderItem orderItem = new OrderItem();
    orderItem.setQuantity(2);

    when(userServiceClient.getUserByEmail(
        "user@example.com"
    )).thenReturn(user);

    when(orderMapper.toEntity(request))
        .thenReturn(order);

    when(itemRepository.findAllById(List.of(11L)))
        .thenReturn(List.of(item));

    when(orderMapper.toEntity(line))
        .thenReturn(orderItem);

    when(orderRepository.save(order))
        .thenReturn(order);

    when(orderMapper.toResponse(order))
        .thenReturn(mappedResponse);

    OrderResponse result = service.createOrder(
        request,
        7L,
        "user@example.com"
    );

    assertEquals(user, result.user());

    assertEquals(7L, order.getUserId());

    assertEquals(
        "user@example.com",
        order.getUserEmail()
    );

    assertEquals(
        new BigDecimal("25.00"),
        order.getTotalPrice()
    );

    assertSame(order, orderItem.getOrder());
    assertSame(item, orderItem.getItem());

    verify(userServiceClient)
        .getUserByEmail("user@example.com");

    verify(orderRepository).save(order);
  }

  @Test
  void createOrderRejectsMismatchedUser() {

    when(userServiceClient.getUserByEmail(
        "user@example.com"
    )).thenReturn(
        new UserInfoResponse(
            999L,
            "user@example.com",
            "Test",
            "User"
        )
    );

    assertThrows(
        AccessDeniedException.class,
        () -> service.createOrder(
            new CreateOrderRequest(List.of(
                new CreateOrderRequest.OrderItemRequest(
                    11L,
                    1
                )
            )),
            7L,
            "user@example.com"
        )
    );

    verifyNoInteractions(orderRepository);
  }

  @Test
  void createOrderFailsWhenItemDoesNotExist() {

    var request = new CreateOrderRequest(List.of(
        new CreateOrderRequest.OrderItemRequest(
            999L,
            1
        )
    ));

    when(userServiceClient.getUserByEmail(
        "user@example.com"
    )).thenReturn(user);

    when(orderMapper.toEntity(request))
        .thenReturn(order);

    when(itemRepository.findAllById(List.of(999L)))
        .thenReturn(List.of());

    assertThrows(
        NotFoundException.class,
        () -> service.createOrder(
            request,
            7L,
            "user@example.com"
        )
    );

    verify(orderRepository, never()).save(any());
  }


  @Test
  void getOrderByIdReturnsOrderWithUser() {

    when(orderRepository.findByIdAndDeletedFalse(21L))
        .thenReturn(Optional.of(order));

    when(userServiceClient.getUserById(7L))
        .thenReturn(user);

    when(orderMapper.toResponse(order))
        .thenReturn(mappedResponse);

    OrderResponse result =
        service.getOrderById(21L);

    assertEquals(user, result.user());
  }

  @Test
  void getOrderByIdThrowsWhenMissingOrDeleted() {

    when(orderRepository.findByIdAndDeletedFalse(99L))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> service.getOrderById(99L)
    );

    verifyNoInteractions(userServiceClient);
  }


  @Test
  void getOrdersRejectsInvalidDateRange() {

    Instant from =
        Instant.parse("2025-02-02T00:00:00Z");

    Instant to =
        Instant.parse("2025-02-01T00:00:00Z");

    assertThrows(
        IllegalArgumentException.class,
        () -> service.getOrders(
            from,
            to,
            Set.of(OrderStatus.CREATED),
            PageRequest.of(0, 10)
        )
    );

    verifyNoInteractions(orderRepository);
  }

  @Test
  void getOrdersReturnsEmptyPageWithoutCallingUserService() {

    Pageable pageable = PageRequest.of(0, 10);

    when(orderRepository.findAll(
        any(Specification.class),
        eq(pageable)
    )).thenReturn(Page.empty(pageable));

    Page<OrderResponse> result = service.getOrders(
        null,
        null,
        Set.of(),
        pageable
    );

    assertTrue(result.isEmpty());

    verifyNoInteractions(userServiceClient);
  }

  @Test
  void getOrdersFetchesUsersOnlyOnceForEntirePage() {

    Pageable pageable = PageRequest.of(0, 10);

    Order secondOrder = new Order();
    secondOrder.setId(22L);
    secondOrder.setUserId(7L);
    secondOrder.setStatus(OrderStatus.CREATED);

    Order thirdOrder = new Order();
    thirdOrder.setId(23L);
    thirdOrder.setUserId(8L);
    thirdOrder.setStatus(OrderStatus.CREATED);

    Page<Order> orders = new PageImpl<>(
        List.of(order, secondOrder, thirdOrder),
        pageable,
        3
    );

    when(orderRepository.findAll(
        any(Specification.class),
        eq(pageable)
    )).thenReturn(orders);

    when(userServiceClient.getUsersByIds(
        List.of(7L, 8L)
    )).thenReturn(List.of(user, secondUser));

    when(orderMapper.toResponse(any(Order.class)))
        .thenReturn(mappedResponse);

    Page<OrderResponse> result = service.getOrders(
        null,
        null,
        Set.of(OrderStatus.CREATED),
        pageable
    );

    assertEquals(3, result.getTotalElements());

    assertEquals(
        user,
        result.getContent().get(0).user()
    );

    assertEquals(
        user,
        result.getContent().get(1).user()
    );

    assertEquals(
        secondUser,
        result.getContent().get(2).user()
    );

    verify(userServiceClient, times(1))
        .getUsersByIds(List.of(7L, 8L));

    verify(userServiceClient, never())
        .getUserById(anyLong());
  }


  @Test
  void getOrdersByUserIdUsesBatchUserLookup() {

    Pageable pageable = PageRequest.of(0, 10);

    when(orderRepository.findAll(
        any(Specification.class),
        eq(pageable)
    )).thenReturn(
        new PageImpl<>(List.of(order), pageable, 1)
    );

    when(userServiceClient.getUsersByIds(
        List.of(7L)
    )).thenReturn(List.of(user));

    when(orderMapper.toResponse(order))
        .thenReturn(mappedResponse);

    Page<OrderResponse> result =
        service.getOrdersByUserId(7L, pageable);

    assertEquals(1, result.getTotalElements());

    assertEquals(
        user,
        result.getContent().getFirst().user()
    );

    verify(userServiceClient)
        .getUsersByIds(List.of(7L));
  }


  @Test
  void updateOrderReplacesLinesAndRecalculatesTotal() {

    var line =
        new CreateOrderRequest.OrderItemRequest(11L, 3);

    var request = new UpdateOrderRequest(
        OrderStatus.PROCESSING,
        List.of(line)
    );

    OrderItem newLine = new OrderItem();
    newLine.setQuantity(3);

    OrderItem oldLine = new OrderItem();
    order.addItem(oldLine);

    when(orderRepository.findByIdAndDeletedFalse(21L))
        .thenReturn(Optional.of(order));

    when(userServiceClient.getUserByEmail("user@example.com"))
        .thenReturn(user);

    doAnswer(invocation -> {
      order.setStatus(request.status());
      return null;
    }).when(orderMapper).updateEntity(request, order);

    when(itemRepository.findAllById(List.of(11L)))
        .thenReturn(List.of(item));

    when(orderMapper.toEntity(line))
        .thenReturn(newLine);

    when(orderRepository.save(order))
        .thenReturn(order);

    when(orderMapper.toResponse(order))
        .thenReturn(mappedResponse);

    OrderResponse result =
        service.updateOrder(21L, request);

    assertEquals(user, result.user());

    assertEquals(
        OrderStatus.PROCESSING,
        order.getStatus()
    );

    assertEquals(
        new BigDecimal("37.50"),
        order.getTotalPrice()
    );

    assertEquals(1, order.getItems().size());

    assertSame(newLine, order.getItems().getFirst());
    assertSame(order, newLine.getOrder());

    assertNull(oldLine.getOrder());
  }

  @Test
  void updateOrderThrowsWhenOrderMissing() {

    when(orderRepository.findByIdAndDeletedFalse(99L))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> service.updateOrder(
            99L,
            new UpdateOrderRequest(
                OrderStatus.PROCESSING,
                List.of(
                    new CreateOrderRequest.OrderItemRequest(
                        11L,
                        1
                    )
                )
            )
        )
    );

    verifyNoInteractions(userServiceClient);
  }

  @Test
  void updateOrderThrowsWhenReplacementItemMissing() {

    var request = new UpdateOrderRequest(
        OrderStatus.PROCESSING,
        List.of(
            new CreateOrderRequest.OrderItemRequest(
                999L,
                1
            )
        )
    );

    when(orderRepository.findByIdAndDeletedFalse(21L))
        .thenReturn(Optional.of(order));

    when(userServiceClient.getUserByEmail("user@example.com"))
        .thenReturn(user);

    when(itemRepository.findAllById(List.of(999L)))
        .thenReturn(List.of());

    assertThrows(
        NotFoundException.class,
        () -> service.updateOrder(21L, request)
    );

    verify(orderRepository, never()).save(any());
  }


  @Test
  void deleteOrderPerformsSoftDelete() {

    when(orderRepository.findByIdAndDeletedFalse(21L))
        .thenReturn(Optional.of(order));

    service.deleteOrder(21L);

    assertTrue(order.isDeleted());

    verify(orderRepository).save(order);
  }

  @Test
  void deleteOrderThrowsWhenMissing() {

    when(orderRepository.findByIdAndDeletedFalse(99L))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> service.deleteOrder(99L)
    );

    verify(orderRepository, never()).save(any());
  }
}
