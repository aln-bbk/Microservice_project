package org.course_project.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateOrderRequest(

    @NotEmpty
    List<@NotNull @Valid OrderItemRequest> items
) {
  public record OrderItemRequest(

      @NotNull
      @Positive
      Long itemId,

      @NotNull
      @Positive
      Integer quantity
  ) {}
}
