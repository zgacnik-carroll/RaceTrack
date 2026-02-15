package com.racetrack.controller;

import com.racetrack.model.RunningLogForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RunningLogController {

    @GetMapping("/running-log/new")
    public String showRunningLogForm(Model model) {
        model.addAttribute("runningLog", new RunningLogForm());
        return "running_form";
    }

    @PostMapping("/running-log")
    public String submitRunningLog(RunningLogForm runningLog) {

        // TEMP: Replace with service + repository
        System.out.println(runningLog);

        return "redirect:/running-log/new?success";
    }
}
