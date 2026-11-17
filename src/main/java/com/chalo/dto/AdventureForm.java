package com.chalo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Backing object for the Host Adventure form (/adventures/new).
 * Field names map onto the Adventure entity in AdventureController:
 *   location       -> locationName
 *   availableSlots -> maxParticipants
 *   tagIds         -> tags (via TagRepository.findByIdIn)
 *   photoUrls      -> photos (cascaded AdventurePhoto rows)
 *
 * WHAT IS A DTO / "FORM OBJECT"?
 *   A Data Transfer Object carries data between the HTML form and the controller.
 *   We do NOT bind the browser form directly to the Adventure entity because users
 *   must never be able to set fields like "host" or "status" by forging a request.
 *   The validation annotations below produce the red error messages on the form.
 *
 * @Data (Lombok) = getters + setters + toString + equals + hashCode for every field.
 */
@Data
public class AdventureForm {

    /** Headline. Must not be empty, max 150 chars. */
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    /** Long description. Must not be empty. */
    @NotBlank(message = "Description is required")
    private String description;

    /** Free-text place name. Saved into Adventure.locationName. */
    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    /** Date of the trip; today or later. @DateTimeFormat accepts the browser's yyyy-MM-dd value. */
    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Date must be today or in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate adventureDate;

    /** Number of seats (1 to 1000). Saved into Adventure.maxParticipants. */
    @NotNull(message = "Available slots is required")
    @Min(value = 1, message = "There must be at least 1 slot")
    @Max(value = 1000, message = "That's a lot of slots — keep it under 1000")
    private Integer availableSlots;

    // At least one interest must be selected
    /** IDs of the ticked interest checkboxes (Tag.id). */
    @NotEmpty(message = "Select at least one interest")
    private List<Long> tagIds = new ArrayList<>();

    // Optional image URLs (MVP). Blank entries are filtered server-side.
    /** Image web addresses typed by the host. The first one becomes the cover image. */
    private List<String> photoUrls = new ArrayList<>();
}
