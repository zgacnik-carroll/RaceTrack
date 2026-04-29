package edu.carroll.racetrack.repository;

import edu.carroll.racetrack.model.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    List<WorkoutLog> findByUserIdOrderByLogDateDesc(Long userId);

    /**
     * Finds a workout log by id only when it belongs to the provided user.
     *
     * @param id workout log id
     * @param userId owning user id
     * @return optional workout log that matches id and owner
     */
    Optional<WorkoutLog> findByIdAndUserId(Long id, Long userId);

    /**
     * Deletes all workout logs owned by the provided user.
     *
     * @param userId owning user id
     */
    @Modifying
    void deleteByUserId(Long userId);
}


