package org.course_project.order_service.dto;

public record UserInfoResponse(
    Long id,
    String email,
    String name,
    String surname
) {}
