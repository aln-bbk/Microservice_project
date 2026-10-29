package org.userService.dto.user;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record UserResponse(
    Long id,
    String name,
    String surname,
    LocalDate birthDate,
    String email,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) implements java.io.Serializable {
}
