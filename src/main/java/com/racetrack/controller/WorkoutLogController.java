package com.racetrack.controller;

import com.racetrack.model.User;
import com.racetrack.model.WorkoutLog;
import com.racetrack.service.UserService;
import com.racetrack.service.WorkoutLogService;
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

    private final WorkoutLogService workoutLogService;
    private final UserService userService;

    /**
     * Creates the workout log controller.
     *
     * @param workoutLogService workout log service
     * @param userService user service
     */
    public WorkoutLogController(WorkoutLogService workoutLogService,
                                UserService userService) {
        this.workoutLogService = workoutLogService;
        this.userService = userService;
    }

    /**
     * Persists a workout log for the authenticated user.
     *
     * @param oidcUser authenticated user
     * @param workoutLog submitted workout log payload
     * @param selectedDate optional date selected from form
     * @return redirect back to home with success flag
     */
    @PostMapping("/workout-log")
    public String submitWorkoutLog(@AuthenticationPrincipal OidcUser oidcUser,
                                   WorkoutLog workoutLog,
                                   @RequestParam(value = "selectedDate", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedDate) {
        User user = userService.getAuthorizedUserForFormSubmit(oidcUser);
        workoutLog.setUser(user);
        workoutLogService.submitWorkoutLog(workoutLog, selectedDate);

        return "redirect:/?workoutSuccess";
    }
}
