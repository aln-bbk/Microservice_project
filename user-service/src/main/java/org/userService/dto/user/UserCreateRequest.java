package org.userService.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserCreateRequest(

    @NotBlank(message = "Name must not be blank")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Surname must not be blank")
    @Size(max = 100, message = "Surname must not exceed 100 characters")
    String surname,

    @Past(message = "Birth date must be in the past")
    LocalDate birthDate,

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must have a valid format")
    @Size(max = 320, message = "Email must not exceed 320 characters")
    String email

) {}