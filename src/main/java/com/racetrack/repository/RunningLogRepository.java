package com.racetrack.repository;

import com.racetrack.model.RunningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for running logs.
 */
@Repository
public interface RunningLogRepository extends JpaRepository<RunningLog, Long> {

    List<RunningLog> findByUser_IdOrderByLogDateDesc(String userId);

    Optional<RunningLog> findByIdAndUser_Id(Long id, String userId);
}

