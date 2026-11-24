package org.userService.dto.card;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PaymentCardResponse(
    Long id,
    Long userId,
    String number,
    String holder,
    LocalDate expirationDate,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) implements java.io.Serializable {
}
