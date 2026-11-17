package com.chalo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Backing object of the sign-up form (templates/auth/register.html).
 * Validated in AuthenticationController.processRegistration() and turned into a
 * User in AuthenticationService.register().
 */
@Data
public class RegistrationForm {

    /** Full name (required, max 100). */
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /** Email = login id. Must look like an email address. */
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    /** Age in years (1 to 120). */
    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Please enter a valid age")
    @Max(value = 120, message = "Please enter a valid age")
    private Integer age;

    /** Plain-text password typed by the user (min 8 chars). It is hashed with BCrypt before saving. */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
