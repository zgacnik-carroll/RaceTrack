package com.racetrack.controller;

import com.racetrack.model.User;
import com.racetrack.model.WorkoutLog;
import com.racetrack.repository.UserRepository;
import com.racetrack.repository.WorkoutLogRepository;
import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles form submissions for workout logs.
 */
@Controller
public class WorkoutLogController {

    private final WorkoutLogRepository workoutLogRepository;
    private final UserRepository userRepository;

    public WorkoutLogController(WorkoutLogRepository workoutLogRepository,
                                UserRepository userRepository) {
        this.workoutLogRepository = workoutLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Persists a workout log for the authenticated user.
     */
    @PostMapping("/workout-log")
    public String submitWorkoutLog(@AuthenticationPrincipal OidcUser oidcUser,
                                   WorkoutLog workoutLog,
                                   @RequestParam(value = "logDate", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate logDate) {

        String oktaId = oidcUser.getSubject();

        User user = userRepository.findById(oktaId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(oktaId);
                    newUser.setEmail(oidcUser.getEmail());
                    newUser.setRole("athlete");
                    return userRepository.save(newUser);
                });

        if (logDate != null) {
            workoutLog.setLogDate(logDate.atStartOfDay());
        }
        workoutLog.setUser(user);
        workoutLogRepository.save(workoutLog);

        return "redirect:/?workoutSuccess";
    }
}
