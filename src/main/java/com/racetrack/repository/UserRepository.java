package com.racetrack.repository;

import com.racetrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for users.
 */
public interface UserRepository extends JpaRepository<User, String> {
}
