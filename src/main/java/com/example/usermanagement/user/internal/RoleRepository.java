package com.example.usermanagement.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AppRole} entities.
 * <p>
 * This repository is internal to the user module.
 */
public interface RoleRepository extends JpaRepository<AppRole, Long> {

    /**
     * Finds a role by its name.
     *
     * @param name the role name (e.g., "ROLE_USER", "ROLE_ADMIN")
     * @return an Optional containing the role if found
     */
    Optional<AppRole> findByName(String name);
}
