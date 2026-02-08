package com.racetrack.controller.api;

import com.racetrack.model.RunnerLogRow;
import com.racetrack.service.RunnerLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/runner-log")
public class RunnerLogApiController {

    private final RunnerLogService runnerLogService;

    public RunnerLogApiController(RunnerLogService runnerLogService) {
        this.runnerLogService = runnerLogService;
    }

    @GetMapping("/{runnerId}")
    public List<RunnerLogRow> getRunnerLog(
            @PathVariable int runnerId) {

        return runnerLogService.getRunnerLog(runnerId);
    }
}
