package com.racetrack.repository;

import com.racetrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for users.
 */
public interface UserRepository extends JpaRepository<User, String> {
    List<User> findByRoleIgnoreCaseOrderByFullNameAsc(String role);
}
