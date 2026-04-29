package edu.carroll.racetrack.repository;

import edu.carroll.racetrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for users.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Returns a user by email, ignoring case.
     *
     * @param email email address
     * @return matching user when present
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Checks whether another user exists with the given email.
     *
     * @param email email address
     * @param id user id to exclude
     * @return true when another user already uses the email
     */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Returns users with the given role ordered by full name.
     *
     * @param role logical user role (for example, "athlete")
     * @return ordered users matching the role
     */
    List<User> findByRoleIgnoreCaseOrderByFullNameAsc(String role);

}

