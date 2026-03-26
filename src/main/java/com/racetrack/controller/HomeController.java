package com.racetrack.controller;

import com.racetrack.model.RunningLog;
import com.racetrack.model.User;
import com.racetrack.model.WorkoutLog;
import com.racetrack.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the public home page routes.
 */
@Controller
public class HomeController {
    private final UserService userService;

    /**
     * Creates the home controller.
     *
     * @param userService user domain service
     */
    public HomeController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Renders the home page with empty form models.
     *
     * @param oidcUser authenticated user
     * @param model thymeleaf model for the page
     * @return template name based on role
     */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        User user = userService.getOrCreateForHome(oidcUser);
        boolean isCoach = userService.isCoach(user);
        List<User> athletes = userService.getAthletesOrderedByName();
        if (!isCoach) {
            athletes = athletes.stream()
                    .filter(athlete -> !user.getId().equals(athlete.getId()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("runningLog", new RunningLog());
        model.addAttribute("workoutLog", new WorkoutLog());
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("currentUserRole", isCoach ? "coach" : "athlete");
        model.addAttribute("currentUserName", userService.displayName(user));
        model.addAttribute("isCoach", isCoach);
        model.addAttribute("athletes", athletes);

        return isCoach ? "home_coach" : "home";
    }
}
