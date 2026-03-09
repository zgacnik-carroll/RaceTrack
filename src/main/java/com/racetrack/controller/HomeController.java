package com.racetrack.controller;

import com.racetrack.model.RunningLog;
import com.racetrack.model.WorkoutLog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles the public home page routes.
 */
@Controller
public class HomeController {

    /**
     * Renders the home page with empty form models.
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("runningLog", new RunningLog());
        model.addAttribute("workoutLog", new WorkoutLog());
        return "home";
    }
}
