package com.racetrack.controller;

import com.racetrack.model.WorkoutLogForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class WorkoutLogController {

    @GetMapping("/workout-log/new")
    public String showWorkoutForm(Model model) {
        model.addAttribute("workoutLog", new WorkoutLogForm());
        return "workout_form";
    }

    @PostMapping("/workout-log")
    public String submitWorkoutLog(WorkoutLogForm workoutLog) {

        // TEMP: replace with service + repository
        System.out.println(workoutLog);

        return "redirect:/workout-log/new?success";
    }
}
