package com.example.usermanagement.user.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a user's profile fields.
 * <p>
 * All fields are optional -- only non-null values will be applied.
 *
 * @param displayName the new display name (max 100 characters)
 * @param firstName   the new first name (max 100 characters)
 * @param lastName    the new last name (max 100 characters)
 */
@Schema(description = "Profile update fields (all optional)")
public record ProfileUpdateRequest(
    @Schema(description = "Display name", example = "John Doe")
    @Size(max = 100) String displayName,
    @Schema(description = "First name", example = "John")
    @Size(max = 100) String firstName,
    @Schema(description = "Last name", example = "Doe")
    @Size(max = 100) String lastName
) {
}
