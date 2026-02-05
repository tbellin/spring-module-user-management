package com.example.usermanagement.auth.internal.verification;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter for verification email resend requests.
 * <p>
 * Enforces a cooldown period between resend attempts for the same email
 * address to prevent abuse.
 * <p>
 * Note: This is a simple in-memory implementation. In a clustered environment,
 * consider using Redis or similar distributed cache.
 */
@Component
public class ResendRateLimiter {

    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final ConcurrentHashMap<String, Instant> lastResendTime = new ConcurrentHashMap<>();

    /**
     * Checks if a resend request is allowed for the given email.
     *
     * @param email the email address to check
     * @return true if resend is allowed (cooldown has passed)
     */
    public boolean canResend(String email) {
        String normalizedEmail = email.toLowerCase();
        Instant lastTime = lastResendTime.get(normalizedEmail);

        if (lastTime == null) {
            return true;
        }

        return Instant.now().isAfter(lastTime.plus(COOLDOWN));
    }

    /**
     * Records a resend attempt for the given email.
     * <p>
     * Call this after successfully sending a verification email.
     *
     * @param email the email address that received a resend
     */
    public void recordResend(String email) {
        String normalizedEmail = email.toLowerCase();
        lastResendTime.put(normalizedEmail, Instant.now());
    }

    /**
     * Gets the remaining cooldown time in seconds for a given email.
     *
     * @param email the email address to check
     * @return remaining seconds until resend is allowed, or 0 if allowed now
     */
    public long getRemainingCooldownSeconds(String email) {
        String normalizedEmail = email.toLowerCase();
        Instant lastTime = lastResendTime.get(normalizedEmail);

        if (lastTime == null) {
            return 0;
        }

        Instant cooldownEnd = lastTime.plus(COOLDOWN);
        if (Instant.now().isAfter(cooldownEnd)) {
            return 0;
        }

        return Duration.between(Instant.now(), cooldownEnd).getSeconds();
    }
}
