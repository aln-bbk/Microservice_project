package org.userService.mapper;

import org.userService.dto.card.PaymentCardCreateRequest;
import org.userService.dto.card.PaymentCardResponse;
import org.userService.dto.card.PaymentCardUpdateRequest;
import org.userService.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = org.mapstruct.ReportingPolicy.ERROR
)
public interface PaymentCardMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  PaymentCard toEntity(PaymentCardCreateRequest request);

  @Mapping(target = "userId", source = "user.id")
  PaymentCardResponse toResponse(PaymentCard card);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(
      PaymentCardUpdateRequest request,
      @MappingTarget PaymentCard card
  );
}
