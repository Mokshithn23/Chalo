package com.chalo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Backing object of the "Edit profile" form (templates/profile-edit.html).
 * Used by ProfileController.showEditForm() (pre-fills it) and saveProfile() (validates it).
 * Only name and bio can be edited in this version.
 */
@Getter
@Setter
public class ProfileEditForm {

    /** New display name (required, max 100). */
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be 100 characters or fewer")
    private String name;

    /** New "about me" text (optional, max 500). */
    @Size(max = 500, message = "Bio must be 500 characters or fewer")
    private String bio;
}
