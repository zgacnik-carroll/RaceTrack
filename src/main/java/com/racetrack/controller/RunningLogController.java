package com.racetrack.controller;

import com.racetrack.model.RunningLog;
import com.racetrack.model.User;
import com.racetrack.service.RunningLogService;
import com.racetrack.service.UserService;
import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles form submissions for running logs.
 */
@Controller
public class RunningLogController {

    private final RunningLogService runningLogService;
    private final UserService userService;

    /**
     * Creates the running log controller.
     *
     * @param runningLogService running log service
     * @param userService user service
     */
    public RunningLogController(RunningLogService runningLogService,
                                UserService userService) {
        this.runningLogService = runningLogService;
        this.userService = userService;
    }

    /**
     * Persists a running log for the authenticated user.
     *
     * @param oidcUser authenticated user
     * @param runningLog submitted running log payload
     * @param selectedDate optional date selected from form
     * @return redirect back to home with success flag
     */
    @PostMapping("/running-log")
    public String submitRunningLog(@AuthenticationPrincipal OidcUser oidcUser,
                                   RunningLog runningLog,
                                   @RequestParam(value = "selectedDate", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedDate) {
        User user = userService.getOrCreateForFormSubmit(oidcUser);
        runningLog.setUser(user);
        runningLogService.submitRunningLog(runningLog, selectedDate);

        return "redirect:/?runningSuccess";
    }
}
