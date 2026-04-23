package com.racetrack.service;

import com.racetrack.model.User;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.UserRepository;
import com.racetrack.repository.WorkoutLogRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Coordinates coach-only administrative actions.
 */
@Service
public class AdminService {
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final RunningLogRepository runningLogRepository;
    private final WorkoutLogRepository workoutLogRepository;

    /**
     * Creates the admin service with repositories needed for local user and
     * log maintenance.
     *
     * @param userRepository user persistence access
     * @param runningLogRepository running-log persistence access
     * @param workoutLogRepository workout-log persistence access
     */
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

        // The current admin flow creates a local RaceTrack user only; it does not create an Okta account.
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(normalizedEmail);
        user.setFullName(normalizedFirstName + " " + normalizedLastName);
        user.setRole(normalizedRole);
        User savedUser = userRepository.save(user);
        log.info("Admin service created user userId={} email={} role={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
        return savedUser;
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
        User savedUser = userRepository.save(user);
        log.info("Admin service updated user userId={} email={} role={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
        return savedUser;
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

        // Remove dependent log rows first so the user record can be deleted cleanly.
        workoutLogRepository.deleteByUser_Id(normalizedUserId);
        runningLogRepository.deleteByUser_Id(normalizedUserId);
        userRepository.delete(user);
        log.info("Admin service deleted user userId={}", normalizedUserId);
    }

    /**
     * Removes all log data while preserving all user accounts.
     */
    @Transactional
    public void clearAllLogData() {
        workoutLogRepository.deleteAllInBatch();
        runningLogRepository.deleteAllInBatch();
        log.info("Admin service cleared all running and workout log data");
    }

    /**
     * Normalizes a required string field and raises a validation error when it
     * is null or blank.
     *
     * @param value raw field value
     * @param fieldName human-readable field label
     * @return trimmed, non-empty value
     */
    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        return normalized;
    }

    /**
     * Normalizes and validates an email address used for RaceTrack login matching.
     *
     * @param email raw email value
     * @return trimmed, lowercased email
     */
    private String normalizeEmail(String email) {
        String normalized = normalizeRequired(email, "Email").toLowerCase();
        if (!normalized.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must be valid.");
        }
        return normalized;
    }

    /**
     * Normalizes and validates the supported application roles.
     *
     * @param role raw role value
     * @return normalized role string
     */
    private String normalizeRole(String role) {
        String normalized = normalizeRequired(role, "Role").toLowerCase();
        if (!"athlete".equals(normalized) && !"coach".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be athlete or coach.");
        }
        return normalized;
    }

    /**
     * Rejects a create request when the email is already assigned locally.
     *
     * @param email normalized email to check
     */
    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists.");
        }
    }

    /**
     * Rejects an update request when another local user already owns the email.
     *
     * @param email normalized email to check
     * @param userId current user id that should be excluded from the duplicate check
     */
    private void ensureEmailAvailableForOtherUser(String email, String userId) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists.");
        }
    }

    /**
     * Normalizes optional text values so blank strings are stored as {@code null}.
     *
     * @param value raw field value
     * @return trimmed value or {@code null}
     */
    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
