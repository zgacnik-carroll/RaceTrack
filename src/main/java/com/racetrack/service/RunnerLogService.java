package com.racetrack.service;

import com.racetrack.model.RunnerLogRow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RunnerLogService {

    public List<RunnerLogRow> getRunnerLog(int runnerId) {

        List<RunnerLogRow> rows = new ArrayList<>();

        rows.add(new RunnerLogRow(
                1, "2026-02-01", "Easy Run",
                "5", "40:00", "8:00",
                "Pegasus", "Cold", "Good",
                "8", "140", "165",
                "Felt relaxed"
        ));

        rows.add(new RunnerLogRow(
                2, "2026-02-02", "Intervals",
                "6", "38:00", "6:20",
                "Dragonfly", "Windy", "Hard",
                "7", "155", "182",
                "Legs tired"
        ));

        return rows;
    }
}
