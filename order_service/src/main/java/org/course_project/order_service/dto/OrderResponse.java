package org.course_project.order_service.dto;

import org.course_project.order_service.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    Long userId,
    OrderStatus status,
    BigDecimal totalPrice,
    List<OrderItemResponse> items,
    UserInfoResponse user,
    Instant createdAt,
    Instant updatedAt
) {
}