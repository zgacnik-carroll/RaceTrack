package com.racetrack.repository;

import com.racetrack.model.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Data access for workout logs.
 */
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {

    List<WorkoutLog> findByUser_IdOrderByLogDateDesc(String userId);

    Optional<WorkoutLog> findByIdAndUser_Id(Long id, String userId);
}

