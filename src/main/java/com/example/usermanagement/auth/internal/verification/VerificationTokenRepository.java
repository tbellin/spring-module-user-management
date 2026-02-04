package com.example.usermanagement.auth.internal.verification;

import com.example.usermanagement.user.internal.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for verification token persistence.
 * <p>
 * Internal to the auth module - accessed via VerificationService.
 */
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * Finds a verification token by its token string.
     *
     * @param token the token string to search for
     * @return the token if found
     */
    Optional<VerificationToken> findByToken(String token);

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
    @Query("UPDATE VerificationToken t SET t.used = true WHERE t.token = :token AND t.used = false")
    int markAsUsed(@Param("token") String token);

    /**
     * Finds the most recent unused token for a user.
     *
     * @param user the user to find tokens for
     * @return the most recent unused token if any
     */
    Optional<VerificationToken> findFirstByUserAndUsedFalseOrderByCreatedAtDesc(AppUser user);

    /**
     * Deletes all tokens for a user.
     * <p>
     * Used when issuing a new token to invalidate old ones (per CONTEXT.md requirement).
     *
     * @param user the user whose tokens should be deleted
     */
    void deleteByUser(AppUser user);
}
