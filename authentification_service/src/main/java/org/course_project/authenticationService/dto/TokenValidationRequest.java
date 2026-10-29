package org.course_project.authenticationService.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenValidationRequest(
    @NotBlank String token
) {
}
