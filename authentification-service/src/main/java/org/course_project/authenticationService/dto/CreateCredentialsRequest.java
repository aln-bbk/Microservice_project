package org.course_project.authenticationService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCredentialsRequest(
    @NotNull
    @Positive
    Long userId,

    @NotBlank
    @Size(max = 100)
    String login,

    @NotBlank
    @Size(min = 12, max = 72)
    String password
) {
}