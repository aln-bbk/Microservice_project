package org.course_project.order_service.mapper;

import org.course_project.order_service.dto.*;
import org.course_project.order_service.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OrderMapper {

  @Mapping(target = "user", ignore = true)
  OrderResponse toResponse(Order order);

  @Mapping(target = "itemId", source = "item.id")
  @Mapping(target = "name", source = "item.name")
  @Mapping(target = "price", source = "item.price")
  OrderItemResponse toItemResponse(OrderItem orderItem);

  ItemResponse toItemResponse(Item item);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "userEmail", ignore = true)
  @Mapping(target = "status", constant = "CREATED")
  @Mapping(target = "totalPrice", ignore = true)
  @Mapping(target = "deleted", constant = "false")
  @Mapping(target = "items", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Order toEntity(CreateOrderRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "order", ignore = true)
  @Mapping(target = "item", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  OrderItem toEntity(
      CreateOrderRequest.OrderItemRequest request
  );

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "userEmail", ignore = true)
  @Mapping(target = "totalPrice", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "items", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(
      UpdateOrderRequest request,
      @MappingTarget Order order
  );
}
