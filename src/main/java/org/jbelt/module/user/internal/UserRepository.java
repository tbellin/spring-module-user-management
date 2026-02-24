package org.jbelt.module.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AppUser} entities.
 * <p>
 * This repository is internal to the user module. External modules should use
 * {@link org.jbelt.module.user.UserService} to access user data.
 */
public interface UserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email address
     * @return an Optional containing the user if found
     */
    Optional<AppUser> findByEmail(String email);

    /**
     * Finds a user by their username.
     *
     * @param username the username
     * @return an Optional containing the user if found
     */
    Optional<AppUser> findByUsername(String username);

    /**
     * Checks if a user exists with the given email.
     *
     * @param email the email address
     * @return true if a user exists with this email
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user exists with the given username.
     *
     * @param username the username
     * @return true if a user exists with this username
     */
    boolean existsByUsername(String username);
}
