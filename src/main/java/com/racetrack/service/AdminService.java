package com.racetrack.service;

import com.racetrack.model.User;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.UserRepository;
import com.racetrack.repository.WorkoutLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Coordinates coach-only administrative actions.
 */
@Service
public class AdminService {

    private final OktaAdminClient oktaAdminClient;
    private final UserRepository userRepository;
    private final RunningLogRepository runningLogRepository;
    private final WorkoutLogRepository workoutLogRepository;

    public AdminService(OktaAdminClient oktaAdminClient,
                        UserRepository userRepository,
                        RunningLogRepository runningLogRepository,
                        WorkoutLogRepository workoutLogRepository) {
        this.oktaAdminClient = oktaAdminClient;
        this.userRepository = userRepository;
        this.runningLogRepository = runningLogRepository;
        this.workoutLogRepository = workoutLogRepository;
    }

    /**
     * Creates an athlete in Okta, then upserts the local user record.
     *
     * @param firstName athlete first name
     * @param lastName athlete last name
     * @param email athlete email/login
     * @param temporaryPassword optional temporary password
     * @return saved athlete record
     */
    public User createAthlete(String firstName,
                              String lastName,
                              String email,
                              String temporaryPassword) {
        String normalizedFirstName = normalizeRequired(firstName, "First name");
        String normalizedLastName = normalizeRequired(lastName, "Last name");
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = normalizeOptional(temporaryPassword);

        OktaAdminClient.CreatedOktaUser createdUser = oktaAdminClient.createAthlete(
                normalizedFirstName,
                normalizedLastName,
                normalizedEmail,
                normalizedPassword
        );

        User user = userRepository.findById(createdUser.id()).orElseGet(User::new);
        user.setId(createdUser.id());
        user.setEmail(createdUser.email());
        user.setFullName(normalizedFirstName + " " + normalizedLastName);
        user.setRole("athlete");
        return userRepository.save(user);
    }

    /**
     * Deletes an athlete from Okta and removes the athlete plus owned logs from the local database.
     *
     * @param athleteId athlete user id / Okta id
     */
    @Transactional
    public void deleteAthlete(String athleteId) {
        String normalizedAthleteId = normalizeRequired(athleteId, "Athlete id");
        User athlete = userRepository.findById(normalizedAthleteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found."));

        if (!"athlete".equalsIgnoreCase(athlete.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only athletes can be deleted.");
        }

        oktaAdminClient.deleteUser(normalizedAthleteId);
        workoutLogRepository.deleteByUser_Id(normalizedAthleteId);
        runningLogRepository.deleteByUser_Id(normalizedAthleteId);
        userRepository.delete(athlete);
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

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
