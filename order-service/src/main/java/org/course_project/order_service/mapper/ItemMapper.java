package org.course_project.order_service.mapper;

import org.course_project.order_service.dto.ItemCreateRequest;
import org.course_project.order_service.dto.ItemResponse;
import org.course_project.order_service.dto.ItemUpdateRequest;
import org.course_project.order_service.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Item toEntity(ItemCreateRequest request);

  ItemResponse toResponse(Item item);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(ItemUpdateRequest request, @MappingTarget Item item);
}
