package com.racetrack.repository;

import com.racetrack.model.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Data access for workout logs.
 */
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {

    /**
     * Returns workout logs for the given user sorted newest first.
     *
     * @param userId authenticated user id
     * @return workout logs for the user
     */
    List<WorkoutLog> findByUser_IdOrderByLogDateDesc(String userId);

    /**
     * Finds a workout log by id only when it belongs to the provided user.
     *
     * @param id workout log id
     * @param userId owning user id
     * @return optional workout log that matches id and owner
     */
    Optional<WorkoutLog> findByIdAndUser_Id(Long id, String userId);
}

