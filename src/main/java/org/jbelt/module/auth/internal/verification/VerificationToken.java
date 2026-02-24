package org.jbelt.module.auth.internal.verification;

import org.jbelt.module.user.internal.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity for email verification tokens.
 * <p>
 * Maps to the verification_token table. Tokens are single-use and have
 * an expiration time. Once used or expired, tokens cannot be reused.
 */
@Entity
@Table(name = "verification_token")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected VerificationToken() {
    }

    /**
     * Creates a new verification token.
     *
     * @param token      the unique token string
     * @param user       the user this token belongs to
     * @param expiryDate when this token expires
     */
    public VerificationToken(String token, AppUser user, LocalDateTime expiryDate) {
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Checks if this token has expired.
     *
     * @return true if the current time is after the expiry date
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Checks if this token is still valid (not used and not expired).
     *
     * @return true if the token can be used for verification
     */
    public boolean isValid() {
        return !used && !isExpired();
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public AppUser getUser() {
        return user;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public boolean isUsed() {
        return used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters (limited - token state mostly immutable)

    public void setUsed(boolean used) {
        this.used = used;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VerificationToken that = (VerificationToken) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "VerificationToken{" +
            "id=" + id +
            ", token='" + token.substring(0, Math.min(8, token.length())) + "...'" +
            ", userId=" + (user != null ? user.getId() : null) +
            ", expiryDate=" + expiryDate +
            ", used=" + used +
            '}';
    }
}
