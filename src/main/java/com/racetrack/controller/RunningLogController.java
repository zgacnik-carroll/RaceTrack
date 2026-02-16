package com.racetrack.controller;

import com.racetrack.model.RunningLog;
import com.racetrack.model.User;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RunningLogController {

    private final RunningLogRepository runningLogRepository;
    private final UserRepository userRepository;

    public RunningLogController(RunningLogRepository runningLogRepository,
                                UserRepository userRepository) {
        this.runningLogRepository = runningLogRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/running-log")
    public String submitRunningLog(@AuthenticationPrincipal OidcUser oidcUser,
                                   RunningLog runningLog) {

        String oktaId = oidcUser.getSubject();

        User user = userRepository.findById(oktaId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(oktaId);
                    newUser.setEmail(oidcUser.getEmail());
                    newUser.setRole("athlete");
                    return userRepository.save(newUser);
                });

        runningLog.setUser(user);
        runningLogRepository.save(runningLog);

        return "redirect:/?runningSuccess";
    }
}
