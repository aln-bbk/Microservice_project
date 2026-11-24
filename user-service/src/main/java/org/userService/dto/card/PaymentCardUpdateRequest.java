package org.userService.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PaymentCardUpdateRequest(

    @NotBlank(message = "Card number must not be blank")
    @Pattern(
        regexp = "[0-9]{12,19}",
        message = "Card number must contain 12 to 19 digits"
    )
    String number,

    @NotBlank(message = "Card holder must not be blank")
    @Size(max = 200, message = "Card holder must not exceed 200 characters")
    String holder,

    @NotNull(message = "Expiration date must not be null")
    LocalDate expirationDate

) {}
