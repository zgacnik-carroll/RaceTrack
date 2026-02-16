package com.racetrack.controller;

import com.racetrack.model.User;
import com.racetrack.model.WorkoutLog;
import com.racetrack.repository.UserRepository;
import com.racetrack.repository.WorkoutLogRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class WorkoutLogController {

    private final WorkoutLogRepository workoutLogRepository;
    private final UserRepository userRepository;

    public WorkoutLogController(WorkoutLogRepository workoutLogRepository,
                                UserRepository userRepository) {
        this.workoutLogRepository = workoutLogRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/workout-log")
    public String submitWorkoutLog(@AuthenticationPrincipal OidcUser oidcUser,
                                   WorkoutLog workoutLog) {

        String oktaId = oidcUser.getSubject();

        User user = userRepository.findById(oktaId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(oktaId);
                    newUser.setEmail(oidcUser.getEmail());
                    newUser.setRole("athlete");
                    return userRepository.save(newUser);
                });

        workoutLog.setUser(user);
        workoutLogRepository.save(workoutLog);

        return "redirect:/?workoutSuccess";
    }
}
