package org.userService.dto.card;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PaymentCardSummaryResponse(
    Long id,
    Long userId,
    String maskedNumber,
    String holder,
    LocalDate expirationDate,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
