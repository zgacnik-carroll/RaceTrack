package com.racetrack.controller;

import com.racetrack.service.RunnerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SecureController {

    private final RunnerService runnerService;

    public SecureController(RunnerService runnerService) {
        this.runnerService = runnerService;
    }

    @GetMapping("/secure")
    public String secure(Model model) {
        model.addAttribute("runners",
                runnerService.getAllRunners());
        return "secure";
    }
}
