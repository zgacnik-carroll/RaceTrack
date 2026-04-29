package edu.carroll.racetrack.repository;

import edu.carroll.racetrack.model.RunningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for running logs.
 */
@Repository
public interface RunningLogRepository extends JpaRepository<RunningLog, Long> {

    /**
     * Returns running logs for the given user sorted newest first.
     *
     * @param userId authenticated user id
     * @return running logs for the user
     */
    List<RunningLog> findByUserIdOrderByLogDateDesc(Long userId);

    /**
     * Finds a running log by id only when it belongs to the provided user.
     *
     * @param id running log id
     * @param userId owning user id
     * @return optional running log that matches id and owner
     */
    Optional<RunningLog> findByIdAndUserId(Long id, Long userId);

    /**
     * Deletes all running logs owned by the provided user.
     *
     * @param userId owning user id
     */
    @Modifying
    void deleteByUserId(Long userId);
}


