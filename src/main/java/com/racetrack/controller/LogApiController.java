package com.racetrack.controller;

import com.racetrack.model.RunningLog;
import com.racetrack.model.User;
import com.racetrack.model.WorkoutLog;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.UserRepository;
import com.racetrack.repository.WorkoutLogRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * API endpoints for retrieving running and workout logs.
 */
@RestController
@RequestMapping("/api")
public class LogApiController {

    private final RunningLogRepository runningLogRepository;
    private final WorkoutLogRepository workoutLogRepository;
    private final UserRepository userRepository;

    public LogApiController(RunningLogRepository runningLogRepository,
                            WorkoutLogRepository workoutLogRepository,
                            UserRepository userRepository) {
        this.runningLogRepository = runningLogRepository;
        this.workoutLogRepository = workoutLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns running logs for the authenticated user.
     */
    @GetMapping("/running-logs/me")
    public List<RunningLog> myRunningLogs(@AuthenticationPrincipal OidcUser oidcUser) {
        return runningLogRepository.findByUser_IdOrderByLogDateDesc(oidcUser.getSubject());
    }

    /**
     * Returns running logs for the provided user id.
     * Athletes can only access themselves; coaches can access everyone.
     */
    @GetMapping("/running-logs/{userId}")
    public List<RunningLog> runningLogsByUser(@PathVariable String userId,
                                              @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = getOrCreateUser(oidcUser);
        String targetUserId = resolveTargetUserId(userId, oidcUser.getSubject());
        assertCanViewTargetUser(currentUser, oidcUser.getSubject(), targetUserId);
        return runningLogRepository.findByUser_IdOrderByLogDateDesc(targetUserId);
    }

    /**
     * Returns workout logs for the authenticated user.
     */
    @GetMapping("/workout-logs/me")
    public List<WorkoutLog> myWorkoutLogs(@AuthenticationPrincipal OidcUser oidcUser) {
        return workoutLogRepository.findByUser_IdOrderByLogDateDesc(oidcUser.getSubject());
    }

    /**
     * Returns workout logs for the provided user id.
     * Athletes can only access themselves; coaches can access everyone.
     */
    @GetMapping("/workout-logs/{userId}")
    public List<WorkoutLog> workoutLogsByUser(@PathVariable String userId,
                                              @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = getOrCreateUser(oidcUser);
        String targetUserId = resolveTargetUserId(userId, oidcUser.getSubject());
        assertCanViewTargetUser(currentUser, oidcUser.getSubject(), targetUserId);
        return workoutLogRepository.findByUser_IdOrderByLogDateDesc(targetUserId);
    }

    /**
     * Athlete-only: update own running log row values from the spreadsheet view.
     */
    @PutMapping("/running-logs/{id}")
    public ResponseEntity<Void> updateRunningLog(@PathVariable Long id,
                                                 @RequestBody RunningLogUpdateRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = getOrCreateUser(oidcUser);
        if (isCoach(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Coaches cannot edit athlete rows.");
        }

        RunningLog log = runningLogRepository.findByIdAndUser_Id(id, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Running log not found."));

        log.setMileage(request.mileage());
        log.setHurting(request.hurting());
        log.setSleepHours(request.sleepHours());
        log.setStressLevel(request.stressLevel());
        log.setPlateProportion(request.plateProportion());
        log.setGotThatBread(request.gotThatBread());
        log.setFeel(request.feel());
        log.setRpe(request.rpe());
        log.setDetails(request.details());
        runningLogRepository.save(log);

        return ResponseEntity.ok().build();
    }

    /**
     * Athlete-only: update own workout log row values from the spreadsheet view.
     */
    @PutMapping("/workout-logs/{id}")
    public ResponseEntity<Void> updateWorkoutLog(@PathVariable Long id,
                                                 @RequestBody WorkoutLogUpdateRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = getOrCreateUser(oidcUser);
        if (isCoach(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Coaches cannot edit athlete rows.");
        }

        WorkoutLog log = workoutLogRepository.findByIdAndUser_Id(id, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log not found."));

        log.setWorkoutType(request.workoutType());
        log.setCompletionDetails(request.completionDetails());
        log.setActualPaces(request.actualPaces());
        log.setWorkoutDescription(request.workoutDescription());
        workoutLogRepository.save(log);

        return ResponseEntity.ok().build();
    }

    /**
     * Coach-only: add/update a comment on a running log row.
     */
    @PutMapping("/running-logs/{id}/coach-comment")
    public ResponseEntity<Void> updateRunningCoachComment(@PathVariable Long id,
                                                          @RequestBody Map<String, String> request,
                                                          @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = getOrCreateUser(oidcUser);
        if (!isCoach(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can add comments.");
        }

        RunningLog log = runningLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Running log not found."));
        log.setCoachComment(normalizeText(request.get("coachComment")));
        runningLogRepository.save(log);

        return ResponseEntity.ok().build();
    }

    /**
     * Coach-only: add/update a comment on a workout log row.
     */
    @PutMapping("/workout-logs/{id}/coach-comment")
    public ResponseEntity<Void> updateWorkoutCoachComment(@PathVariable Long id,
                                                          @RequestBody Map<String, String> request,
                                                          @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = getOrCreateUser(oidcUser);
        if (!isCoach(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can add comments.");
        }

        WorkoutLog log = workoutLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log not found."));
        log.setCoachComment(normalizeText(request.get("coachComment")));
        workoutLogRepository.save(log);

        return ResponseEntity.ok().build();
    }

    private User getOrCreateUser(OidcUser oidcUser) {
        String oktaId = oidcUser.getSubject();
        return userRepository.findById(oktaId)
                .map(existing -> {
                    if (existing.getEmail() == null || existing.getEmail().isBlank()) {
                        existing.setEmail(oidcUser.getEmail());
                    }
                    String name = oidcUser.getFullName();
                    if (name != null && !name.isBlank() &&
                            (existing.getFullName() == null || existing.getFullName().isBlank())) {
                        existing.setFullName(name);
                    }
                    if (existing.getRole() == null || existing.getRole().isBlank()) {
                        existing.setRole("athlete");
                    }
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(oktaId);
                    newUser.setEmail(oidcUser.getEmail());
                    newUser.setFullName(oidcUser.getFullName());
                    newUser.setRole("athlete");
                    return userRepository.save(newUser);
                });
    }

    private String resolveTargetUserId(String userIdPath, String authenticatedUserId) {
        if ("me".equalsIgnoreCase(userIdPath)) {
            return authenticatedUserId;
        }
        return userIdPath;
    }

    private void assertCanViewTargetUser(User currentUser, String authenticatedUserId, String targetUserId) {
        if (isCoach(currentUser)) {
            return;
        }

        if (!authenticatedUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Athletes can only view their own logs.");
        }
    }

    private boolean isCoach(User user) {
        return user.getRole() != null && "coach".equalsIgnoreCase(user.getRole());
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record RunningLogUpdateRequest(
            Double mileage,
            Boolean hurting,
            Integer sleepHours,
            Integer stressLevel,
            Boolean plateProportion,
            Boolean gotThatBread,
            String feel,
            Integer rpe,
            String details
    ) {}

    public record WorkoutLogUpdateRequest(
            String workoutType,
            String completionDetails,
            String actualPaces,
            String workoutDescription
    ) {}
}
