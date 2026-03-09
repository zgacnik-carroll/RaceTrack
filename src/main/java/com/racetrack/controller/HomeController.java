package com.racetrack.controller;

import com.racetrack.model.RunningLog;
import com.racetrack.model.User;
import com.racetrack.model.WorkoutLog;
import com.racetrack.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Handles the public home page routes.
 */
@Controller
public class HomeController {
    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Renders the home page with empty form models.
     */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        User user = getOrCreateUser(oidcUser);
        boolean isCoach = isCoach(user);
        List<User> athletes = isCoach
                ? userRepository.findByRoleIgnoreCaseOrderByFullNameAsc("athlete")
                : List.of();

        model.addAttribute("runningLog", new RunningLog());
        model.addAttribute("workoutLog", new WorkoutLog());
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("currentUserRole", isCoach ? "coach" : "athlete");
        model.addAttribute("currentUserName", displayName(user));
        model.addAttribute("isCoach", isCoach);
        model.addAttribute("athletes", athletes);

        return isCoach ? "home_coach" : "home";
    }

    private User getOrCreateUser(OidcUser oidcUser) {
        String oktaId = oidcUser.getSubject();
        return userRepository.findById(oktaId)
                .map(existing -> {
                    existing.setEmail(oidcUser.getEmail());
                    if (oidcUser.getFullName() != null && !oidcUser.getFullName().isBlank()) {
                        existing.setFullName(oidcUser.getFullName());
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

    private boolean isCoach(User user) {
        return user.getRole() != null && "coach".equalsIgnoreCase(user.getRole());
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return "User";
    }
}
