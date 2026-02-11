package com.example.usermanagement.user.internal;

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
public record ProfileUpdateRequest(
    @Size(max = 100) String displayName,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName
) {
}
