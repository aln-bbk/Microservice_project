package org.course_project.order_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemResponse(
    Long id,
    String name,
    BigDecimal price,
    Instant createdAt,
    Instant updatedAt
) {}
