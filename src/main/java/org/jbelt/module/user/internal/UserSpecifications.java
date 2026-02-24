package org.jbelt.module.user.internal;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification builders for dynamic {@link AppUser} queries.
 * <p>
 * Provides composable search and filter criteria for the admin user list.
 * All filter parameters are optional; when null or blank they are ignored.
 */
public final class UserSpecifications {

    private UserSpecifications() {
        // Utility class -- no instantiation
    }

    /**
     * Creates a composite specification that filters users by search term, role, and status.
     * <p>
     * All parameters are optional. When provided:
     * <ul>
     *   <li>search: matches against email, firstName, or lastName (case-insensitive, partial match)</li>
     *   <li>role: matches users with the specified role name (e.g., "ROLE_USER", "ROLE_ADMIN")</li>
     *   <li>status: "active" matches enabled=true, "disabled" matches enabled=false</li>
     * </ul>
     * All active filters are combined with AND.
     *
     * @param search partial text to match against email, firstName, or lastName
     * @param role   role name to filter by (e.g., "ROLE_ADMIN")
     * @param status account status: "active" or "disabled"
     * @return a specification combining all active filters
     */
    public static Specification<AppUser> withFilters(String search, String role, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), pattern);
                predicates.add(cb.or(emailMatch, firstNameMatch, lastNameMatch));
            }

            if (role != null && !role.isBlank()) {
                Join<AppUser, AppRole> rolesJoin = root.join("roles");
                predicates.add(cb.equal(rolesJoin.get("name"), role));
            }

            if (status != null && !status.isBlank()) {
                if ("active".equalsIgnoreCase(status)) {
                    predicates.add(cb.isTrue(root.get("enabled")));
                } else if ("disabled".equalsIgnoreCase(status)) {
                    predicates.add(cb.isFalse(root.get("enabled")));
                }
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
