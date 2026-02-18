package com.racetrack.repository;

import com.racetrack.model.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {

    List<WorkoutLog> findByUser_IdOrderByLogDateDesc(String userId);
}

