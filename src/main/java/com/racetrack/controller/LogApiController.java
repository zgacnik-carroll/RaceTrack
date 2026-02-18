package com.racetrack.controller;

import com.racetrack.model.RunningLog;
import com.racetrack.model.WorkoutLog;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.WorkoutLogRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/running-logs/me")
    public List<RunningLog> myRunningLogs(@AuthenticationPrincipal OidcUser oidcUser) {
        return runningLogRepository
                .findByUser_IdOrderByLogDateDesc(oidcUser.getSubject());
    }

    @GetMapping("/workout-logs/me")
    public List<WorkoutLog> myWorkoutLogs(@AuthenticationPrincipal OidcUser oidcUser) {
        return workoutLogRepository
                .findByUser_IdOrderByLogDateDesc(oidcUser.getSubject());
    }

}
