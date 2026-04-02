package com.racetrack.controller;

import com.racetrack.model.RunningLog;
import com.racetrack.model.User;
import com.racetrack.model.WorkoutLog;
import com.racetrack.service.RunningLogService;
import com.racetrack.service.UserService;
import com.racetrack.service.WorkoutLogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * API endpoints for retrieving running and workout logs.
 */
@RestController
@RequestMapping("/api")
public class LogApiController {

    private final RunningLogService runningLogService;
    private final WorkoutLogService workoutLogService;
    private final UserService userService;

    /**
     * Creates the log API controller.
     *
     * @param runningLogService running log service
     * @param workoutLogService workout log service
     * @param userService user service
     */
    public LogApiController(RunningLogService runningLogService,
                            WorkoutLogService workoutLogService,
                            UserService userService) {
        this.runningLogService = runningLogService;
        this.workoutLogService = workoutLogService;
        this.userService = userService;
    }

    /**
     * Returns running logs for the authenticated user.
     *
     * @param oidcUser authenticated user
     * @return running logs for the current user
     */
    @GetMapping("/running-logs/me")
    public List<RunningLog> myRunningLogs(@AuthenticationPrincipal OidcUser oidcUser) {
        return runningLogService.findByUserId(oidcUser.getSubject());
    }

    /**
     * Returns running logs for the provided user id.
     * Athletes can only access themselves; coaches can access everyone.
     *
     * @param userId path user id or "me"
     * @param oidcUser authenticated user
     * @return running logs for the resolved target user
     */
    @GetMapping("/running-logs/{userId}")
    public List<RunningLog> runningLogsByUser(@PathVariable String userId,
                                              @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getOrCreateForApi(oidcUser);
        String targetUserId = resolveTargetUserId(userId, oidcUser.getSubject());
        assertCanViewTargetUser(currentUser, oidcUser.getSubject(), targetUserId);
        return runningLogService.findByUserId(targetUserId);
    }

    /**
     * Returns workout logs for the authenticated user.
     *
     * @param oidcUser authenticated user
     * @return workout logs for the current user
     */
    @GetMapping("/workout-logs/me")
    public List<WorkoutLog> myWorkoutLogs(@AuthenticationPrincipal OidcUser oidcUser) {
        return workoutLogService.findByUserId(oidcUser.getSubject());
    }

    /**
     * Returns workout logs for the provided user id.
     * Athletes can only access themselves; coaches can access everyone.
     *
     * @param userId path user id or "me"
     * @param oidcUser authenticated user
     * @return workout logs for the resolved target user
     */
    @GetMapping("/workout-logs/{userId}")
    public List<WorkoutLog> workoutLogsByUser(@PathVariable String userId,
                                              @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getOrCreateForApi(oidcUser);
        String targetUserId = resolveTargetUserId(userId, oidcUser.getSubject());
        assertCanViewTargetUser(currentUser, oidcUser.getSubject(), targetUserId);
        return workoutLogService.findByUserId(targetUserId);
    }

    /**
     * Athlete-only: update own running log row values from the spreadsheet view.
     *
     * @param id running log id
     * @param request row update payload
     * @param oidcUser authenticated user
     * @return empty success response
     */
    @PutMapping("/running-logs/{id}")
    public ResponseEntity<Void> updateRunningLog(@PathVariable Long id,
                                                 @RequestBody RunningLogUpdateRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getOrCreateForApi(oidcUser);
        if (userService.isCoach(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Coaches cannot edit athlete rows.");
        }

        runningLogService.updateAthleteOwnedLog(
                oidcUser.getSubject(),
                id,
                request.mileage(),
                request.hurting(),
                request.painDetails(),
                request.sleepHours(),
                request.stressLevel(),
                request.plateProportion(),
                request.gotThatBread(),
                request.feel(),
                request.rpe(),
                request.details(),
                request.logDate()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Athlete-only: update own workout log row values from the spreadsheet view.
     *
     * @param id workout log id
     * @param request row update payload
     * @param oidcUser authenticated user
     * @return empty success response
     */
    @PutMapping("/workout-logs/{id}")
    public ResponseEntity<Void> updateWorkoutLog(@PathVariable Long id,
                                                 @RequestBody WorkoutLogUpdateRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getOrCreateForApi(oidcUser);
        if (userService.isCoach(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Coaches cannot edit athlete rows.");
        }

        workoutLogService.updateAthleteOwnedLog(
                oidcUser.getSubject(),
                id,
                request.workoutType(),
                request.completionDetails(),
                request.actualPaces(),
                request.workoutDescription(),
                request.logDate()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Athlete-only: delete own running log row.
     *
     * @param id running log id
     * @param oidcUser authenticated user
     * @return empty success response
     */
    @DeleteMapping("/running-logs/{id}")
    public ResponseEntity<Void> deleteRunningLog(@PathVariable Long id,
                                                 @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getOrCreateForApi(oidcUser);
        if (userService.isCoach(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Coaches cannot delete athlete rows.");
        }

        runningLogService.deleteAthleteOwnedLog(oidcUser.getSubject(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * Athlete-only: delete own workout log row.
     *
     * @param id workout log id
     * @param oidcUser authenticated user
     * @return empty success response
     */
    @DeleteMapping("/workout-logs/{id}")
    public ResponseEntity<Void> deleteWorkoutLog(@PathVariable Long id,
                                                 @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getOrCreateForApi(oidcUser);
        if (userService.isCoach(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Coaches cannot delete athlete rows.");
        }

        workoutLogService.deleteAthleteOwnedLog(oidcUser.getSubject(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * Coach-only: add/update a comment on a running log row.
     *
     * @param id running log id
     * @param request coach comment payload
     * @param oidcUser authenticated user
     * @return empty success response
     */
    @PutMapping("/running-logs/{id}/coach-comment")
    public ResponseEntity<Void> updateRunningCoachComment(@PathVariable Long id,
                                                          @RequestBody Map<String, String> request,
                                                          @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getOrCreateForApi(oidcUser);
        if (!userService.isCoach(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can add comments.");
        }

        runningLogService.updateCoachComment(id, request.get("coachComment"));

        return ResponseEntity.ok().build();
    }

    /**
     * Coach-only: add/update a comment on a workout log row.
     *
     * @param id workout log id
     * @param request coach comment payload
     * @param oidcUser authenticated user
     * @return empty success response
     */
    @PutMapping("/workout-logs/{id}/coach-comment")
    public ResponseEntity<Void> updateWorkoutCoachComment(@PathVariable Long id,
                                                          @RequestBody Map<String, String> request,
                                                          @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = userService.getOrCreateForApi(oidcUser);
        if (!userService.isCoach(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can add comments.");
        }

        workoutLogService.updateCoachComment(id, request.get("coachComment"));

        return ResponseEntity.ok().build();
    }

    /**
     * Resolves "me" path value to the authenticated user id.
     *
     * @param userIdPath raw path variable
     * @param authenticatedUserId current user id
     * @return concrete target user id
     */
    private String resolveTargetUserId(String userIdPath, String authenticatedUserId) {
        if ("me".equalsIgnoreCase(userIdPath)) {
            return authenticatedUserId;
        }
        return userIdPath;
    }

    /**
     * Enforces viewing permissions for athlete vs coach roles.
     *
     * @param currentUser current user entity
     * @param authenticatedUserId authenticated user id
     * @param targetUserId requested target user id
     */
    private void assertCanViewTargetUser(User currentUser, String authenticatedUserId, String targetUserId) {
        // All authenticated users may view athlete logs. Edit/delete permissions remain owner-only,
        // and coach-comment permissions remain coach-only.
    }

    /**
     * Request payload for updating athlete-owned running logs.
     *
     * @param mileage mileage value
     * @param hurting hurting flag
     * @param painDetails hurting details
     * @param sleepHours sleep hours
     * @param stressLevel stress level
     * @param plateProportion plate flag
     * @param gotThatBread carb flag
     * @param feel feel text
     * @param rpe effort score
     * @param details details text
     * @param logDate log date override
     */
    public record RunningLogUpdateRequest(
            Double mileage,
            Boolean hurting,
            String painDetails,
            Integer sleepHours,
            Integer stressLevel,
            Boolean plateProportion,
            Boolean gotThatBread,
            String feel,
            Integer rpe,
            String details,
            LocalDate logDate
    ) {}

    /**
     * Request payload for updating athlete-owned workout logs.
     *
     * @param workoutType workout type
     * @param completionDetails completion details
     * @param actualPaces actual paces
     * @param workoutDescription workout description
     * @param logDate log date override
     */
    public record WorkoutLogUpdateRequest(
            String workoutType,
            String completionDetails,
            String actualPaces,
            String workoutDescription,
            LocalDate logDate
    ) {}
}
