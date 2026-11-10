package org.course_project.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.course_project.order_service.dto.CreateOrderRequest.OrderItemRequest;
import org.course_project.order_service.entity.OrderStatus;

import java.util.List;

public record UpdateOrderRequest(

    @NotNull
    OrderStatus status,

    @NotEmpty
    List<@NotNull @Valid OrderItemRequest> items

) {
}