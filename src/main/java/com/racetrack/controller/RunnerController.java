package com.racetrack.controller;

import com.racetrack.service.RunnerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RunnerController {

    private final RunnerService runnerService;

    public RunnerController(RunnerService runnerService) {
        this.runnerService = runnerService;
    }

    @GetMapping("/runner/{id}")
    public String runnerLog(@PathVariable int id, Model model) {
        model.addAttribute("runnerId", id);
        model.addAttribute("runners", runnerService.getAllRunners());
        return "runner-log";
    }

}
