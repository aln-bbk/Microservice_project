package org.course_project.order_service.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long itemId,
    String name,
    BigDecimal price,
    Integer quantity
) {}
