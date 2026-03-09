package com.racetrack.controller;

import com.racetrack.model.RunningLog;
import com.racetrack.model.User;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.UserRepository;
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

    private final RunningLogRepository runningLogRepository;
    private final UserRepository userRepository;

    public RunningLogController(RunningLogRepository runningLogRepository,
                                UserRepository userRepository) {
        this.runningLogRepository = runningLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Persists a running log for the authenticated user.
     */
    @PostMapping("/running-log")
    public String submitRunningLog(@AuthenticationPrincipal OidcUser oidcUser,
                                   RunningLog runningLog,
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
            runningLog.setLogDate(logDate.atStartOfDay());
        }
        runningLog.setUser(user);
        runningLogRepository.save(runningLog);

        return "redirect:/?runningSuccess";
    }
}
