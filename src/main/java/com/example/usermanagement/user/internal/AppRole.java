package com.example.usermanagement.user.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * JPA entity mapping to the app_role table.
 * <p>
 * Roles are immutable after creation. The standard roles (ROLE_USER, ROLE_ADMIN)
 * are seeded via Flyway migration.
 * <p>
 * This entity is internal to the user module.
 */
@Entity
@Table(name = "app_role")
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected AppRole() {
    }

    /**
     * Creates a new role with the specified name.
     *
     * @param name the role name (e.g., "ROLE_USER", "ROLE_ADMIN")
     */
    public AppRole(String name) {
        this.name = name;
    }

    // Getters only (roles are immutable after creation)

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // equals and hashCode based on id only (null-safe)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppRole appRole = (AppRole) o;
        return id != null && Objects.equals(id, appRole.id);
    }

    @Override
    public int hashCode() {
        // Use a constant hash code for transient entities (id is null)
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AppRole{" +
            "id=" + id +
            ", name='" + name + '\'' +
            '}';
    }
}
