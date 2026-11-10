package org.course_project.order_service.service;

import lombok.RequiredArgsConstructor;
import org.course_project.order_service.dto.CreateOrderRequest;
import org.course_project.order_service.dto.OrderResponse;
import org.course_project.order_service.dto.UpdateOrderRequest;
import org.course_project.order_service.dto.UserInfoResponse;
import org.course_project.order_service.entity.Item;
import org.course_project.order_service.entity.Order;
import org.course_project.order_service.entity.OrderItem;
import org.course_project.order_service.entity.OrderStatus;
import org.course_project.order_service.exception.NotFoundException;
import org.course_project.order_service.mapper.OrderMapper;
import org.course_project.order_service.repository.ItemRepository;
import org.course_project.order_service.repository.OrderRepository;
import org.course_project.order_service.repository.OrderSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final ItemRepository itemRepository;
  private final OrderMapper orderMapper;
  private final UserServiceClient userServiceClient;


  @Transactional
  public OrderResponse createOrder(
      CreateOrderRequest request,
      Long authenticatedUserId,
      String authenticatedEmail
  ) {

    UserInfoResponse user = userServiceClient.getUserByEmail(authenticatedEmail);

    if (!authenticatedUserId.equals(user.id())) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Authenticated user does not match user profile"
      );
    }

    Order order = orderMapper.toEntity(request);

    order.setUserId(user.id());
    order.setUserEmail(user.email());

    Map<Long, Item> itemsById = loadItems(request.items());

    for (CreateOrderRequest.OrderItemRequest requestedItem : request.items()) {

      Item item = itemsById.get(requestedItem.itemId());
      OrderItem orderItem = orderMapper.toEntity(requestedItem);

      orderItem.setItem(item);
      order.addItem(orderItem);
    }

    calculateTotalPrice(order);

    Order savedOrder = orderRepository.save(order);

    return toResponseWithUser(savedOrder, user);
  }

  @Transactional(readOnly = true)
  public OrderResponse getOrderById(Long id) {

    Order order = findActiveOrder(id);
    UserInfoResponse user = userServiceClient.getUserById(order.getUserId());

    return toResponseWithUser(order, user);
  }


  @Transactional(readOnly = true)
  public Page<OrderResponse> getOrders(
      Instant from,
      Instant to,
      Set<OrderStatus> statuses,
      Pageable pageable
  ) {

    validateDateRange(from, to);

    Specification<Order> specification = Specification
        .where(OrderSpecifications.notDeleted())
        .and(OrderSpecifications.createdFrom(from))
        .and(OrderSpecifications.createdTo(to))
        .and(OrderSpecifications.hasStatuses(statuses));

    Page<Order> orders =
        orderRepository.findAll(specification, pageable);

    return mapOrdersWithUsers(orders);
  }


  @Transactional(readOnly = true)
  public Page<OrderResponse> getOrdersByUserId(
      Long userId,
      Pageable pageable
  ) {

    Specification<Order> specification = Specification
        .where(OrderSpecifications.notDeleted())
        .and(OrderSpecifications.hasUserId(userId));

    Page<Order> orders = orderRepository.findAll(specification, pageable);

    return mapOrdersWithUsers(orders);
  }


  @Transactional
  public OrderResponse updateOrder(
      Long id,
      UpdateOrderRequest request
  ) {

    Order order = findActiveOrder(id);

    UserInfoResponse user = userServiceClient.getUserByEmail(order.getUserEmail());
    orderMapper.updateEntity(request, order);

    order.removeAllItems();

    Map<Long, Item> itemsById = loadItems(request.items());

    for (CreateOrderRequest.OrderItemRequest requestedItem :
        request.items()) {

      Item item = itemsById.get(requestedItem.itemId());

      OrderItem orderItem = orderMapper.toEntity(requestedItem);

      orderItem.setItem(item);
      order.addItem(orderItem);
    }

    calculateTotalPrice(order);

    Order updatedOrder = orderRepository.save(order);

    return toResponseWithUser(updatedOrder, user);
  }


  @Transactional
  public void deleteOrder(Long id) {

    Order order = findActiveOrder(id);

    order.setDeleted(true);

    orderRepository.save(order);
  }


  private Order findActiveOrder(Long id) {

    return orderRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() ->
            new NotFoundException("Order not found: " + id)
        );
  }


  private Map<Long, Item> loadItems(
      List<CreateOrderRequest.OrderItemRequest> requestedItems
  ) {

    List<Long> itemIds = requestedItems.stream()
        .map(CreateOrderRequest.OrderItemRequest::itemId)
        .distinct()
        .toList();

    Map<Long, Item> itemsById = itemRepository.findAllById(itemIds)
        .stream()
        .collect(Collectors.toMap(
            Item::getId,
            Function.identity()
        ));

    for (Long itemId : itemIds) {
      if (!itemsById.containsKey(itemId)) {
        throw new NotFoundException(
            "Item not found: " + itemId
        );
      }
    }

    return itemsById;
  }


  private void calculateTotalPrice(Order order) {

    BigDecimal total = order.getItems().stream()
        .map(orderItem ->
            orderItem.getItem().getPrice()
                .multiply(
                    BigDecimal.valueOf(
                        orderItem.getQuantity()
                    )
                )
        )
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    order.setTotalPrice(total);
  }


  private OrderResponse toResponseWithUser(
      Order order,
      UserInfoResponse user
  ) {

    OrderResponse response = orderMapper.toResponse(order);

    return new OrderResponse(
        response.id(),
        response.userId(),
        response.status(),
        response.totalPrice(),
        response.items(),
        user,
        response.createdAt(),
        response.updatedAt()
    );
  }


  private void validateDateRange(Instant from, Instant to) {

    if (from != null && to != null && from.isAfter(to)) {
      throw new IllegalArgumentException(
          "'from' date must not be after 'to' date"
      );
    }
  }

  private Page<OrderResponse> mapOrdersWithUsers(
      Page<Order> orders
  ) {

    if (orders.isEmpty()) {
      return orders.map(orderMapper::toResponse);
    }

    List<Long> userIds = orders.getContent().stream()
        .map(Order::getUserId)
        .distinct()
        .toList();

    List<UserInfoResponse> users = userServiceClient.getUsersByIds(userIds);

    Map<Long, UserInfoResponse> usersById = users.stream()
        .collect(Collectors.toMap(
            UserInfoResponse::id,
            Function.identity()
        ));

    return orders.map(order -> {

      UserInfoResponse user = usersById.get(order.getUserId());

      if (user == null) {
        throw new NotFoundException("User not found: " + order.getUserId());
      }

      return toResponseWithUser(order, user);
    });
  }
}
