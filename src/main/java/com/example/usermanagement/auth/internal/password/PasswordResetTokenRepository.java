package com.example.usermanagement.auth.internal.password;

import com.example.usermanagement.user.internal.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for password reset token persistence.
 * <p>
 * Internal to the auth module - accessed via PasswordService.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds a password reset token by its token string.
     *
     * @param token the token string to search for
     * @return the token if found
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Atomically marks a token as used if it hasn't been used yet.
     * <p>
     * This prevents race conditions when the same link is clicked multiple times.
     * Returns 1 if the token was marked used, 0 if it was already used.
     *
     * @param token the token string to mark as used
     * @return number of rows updated (1 if successful, 0 if already used)
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.token = :token AND t.used = false")
    int markAsUsed(@Param("token") String token);

    /**
     * Deletes all tokens for a user.
     * <p>
     * Used when issuing a new token to invalidate old ones.
     *
     * @param user the user whose tokens should be deleted
     */
    void deleteByUser(AppUser user);
}
