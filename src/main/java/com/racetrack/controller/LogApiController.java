package com.racetrack.controller;

import com.racetrack.model.RunningLog;
import com.racetrack.model.WorkoutLog;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.WorkoutLogRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API endpoints for retrieving running and workout logs.
 */
@RestController
@RequestMapping("/api")
public class LogApiController {

    private final RunningLogRepository runningLogRepository;
    private final WorkoutLogRepository workoutLogRepository;

    public LogApiController(RunningLogRepository runningLogRepository,
                            WorkoutLogRepository workoutLogRepository) {
        this.runningLogRepository = runningLogRepository;
        this.workoutLogRepository = workoutLogRepository;
    }

    /**
     * Returns running logs for the authenticated user.
     */
    @GetMapping("/running-logs/me")
    public List<RunningLog> myRunningLogs(@AuthenticationPrincipal OidcUser oidcUser) {
        return runningLogRepository
                .findByUser_IdOrderByLogDateDesc(oidcUser.getSubject());
    }

    /**
     * Returns workout logs for the authenticated user.
     */
    @GetMapping("/workout-logs/me")
    public List<WorkoutLog> myWorkoutLogs(@AuthenticationPrincipal OidcUser oidcUser) {
        return workoutLogRepository
                .findByUser_IdOrderByLogDateDesc(oidcUser.getSubject());
    }

}
