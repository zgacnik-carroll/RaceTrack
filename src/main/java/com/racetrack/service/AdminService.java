package com.racetrack.service;

import com.racetrack.model.User;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.UserRepository;
import com.racetrack.repository.WorkoutLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Coordinates coach-only administrative actions.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RunningLogRepository runningLogRepository;
    private final WorkoutLogRepository workoutLogRepository;

    public AdminService(UserRepository userRepository,
                        RunningLogRepository runningLogRepository,
                        WorkoutLogRepository workoutLogRepository) {
        this.userRepository = userRepository;
        this.runningLogRepository = runningLogRepository;
        this.workoutLogRepository = workoutLogRepository;
    }

    /**
     * Creates an app user in the local database.
     *
     * @param firstName user first name
     * @param lastName user last name
     * @param email user email/login
     * @param role requested app role
     * @return saved user record
     */
    public User createUser(String firstName,
                           String lastName,
                           String email,
                           String role) {
        String normalizedFirstName = normalizeRequired(firstName, "First name");
        String normalizedLastName = normalizeRequired(lastName, "Last name");
        String normalizedEmail = normalizeEmail(email);
        String normalizedRole = normalizeRole(role);
        ensureEmailAvailable(normalizedEmail);

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(normalizedEmail);
        user.setFullName(normalizedFirstName + " " + normalizedLastName);
        user.setRole(normalizedRole);
        return userRepository.save(user);
    }

    /**
     * Updates a local user record.
     *
     * @param athleteId athlete user id / Okta id
     * @param firstName athlete first name
     * @param lastName athlete last name
     * @param email athlete email/login
     * @return updated athlete record
     */
    public User updateUser(String userId,
                           String firstName,
                           String lastName,
                           String email,
                           String role) {
        String normalizedUserId = normalizeRequired(userId, "User id");
        User user = userRepository.findById(normalizedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        String normalizedFirstName = normalizeRequired(firstName, "First name");
        String normalizedLastName = normalizeRequired(lastName, "Last name");
        String normalizedEmail = normalizeEmail(email);
        String normalizedRole = normalizeRole(role);
        ensureEmailAvailableForOtherUser(normalizedEmail, normalizedUserId);

        user.setEmail(normalizedEmail);
        user.setFullName(normalizedFirstName + " " + normalizedLastName);
        user.setRole(normalizedRole);
        return userRepository.save(user);
    }

    /**
     * Removes the user plus owned logs from the local database.
     *
     * @param athleteId athlete user id / Okta id
     */
    @Transactional
    public void deleteUser(String userId) {
        String normalizedUserId = normalizeRequired(userId, "User id");
        User user = userRepository.findById(normalizedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        workoutLogRepository.deleteByUser_Id(normalizedUserId);
        runningLogRepository.deleteByUser_Id(normalizedUserId);
        userRepository.delete(user);
    }

    /**
     * Removes all log data while preserving all user accounts.
     */
    @Transactional
    public void clearAllLogData() {
        workoutLogRepository.deleteAllInBatch();
        runningLogRepository.deleteAllInBatch();
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeRequired(email, "Email").toLowerCase();
        if (!normalized.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must be valid.");
        }
        return normalized;
    }

    private String normalizeRole(String role) {
        String normalized = normalizeRequired(role, "Role").toLowerCase();
        if (!"athlete".equals(normalized) && !"coach".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be athlete or coach.");
        }
        return normalized;
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists.");
        }
    }

    private void ensureEmailAvailableForOtherUser(String email, String userId) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists.");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
