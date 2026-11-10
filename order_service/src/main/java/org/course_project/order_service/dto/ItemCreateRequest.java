package org.course_project.order_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemCreateRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull @DecimalMin(value = "0.00") BigDecimal price
) {}
