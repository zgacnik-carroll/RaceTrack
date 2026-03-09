package com.racetrack.repository;

import com.racetrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for users.
 */
public interface UserRepository extends JpaRepository<User, String> {
    /**
     * Returns users with the given role ordered by full name.
     *
     * @param role logical user role (for example, "athlete")
     * @return ordered users matching the role
     */
    List<User> findByRoleIgnoreCaseOrderByFullNameAsc(String role);
}
