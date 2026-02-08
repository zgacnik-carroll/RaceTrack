package com.racetrack.service;

import com.racetrack.model.Runner;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RunnerService {

    public List<Runner> getAllRunners() {

        return List.of(
                new Runner(1, "Zack"),
                new Runner(2, "Emma"),
                new Runner(3, "Luke")
        );
    }
}
