package org.course_project.authenticationService.dto;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType
) {
}
