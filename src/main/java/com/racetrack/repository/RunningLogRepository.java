package com.racetrack.repository;

import com.racetrack.model.RunningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RunningLogRepository extends JpaRepository<RunningLog, Long> {
    List<RunningLog> findByUserId(String userId);
}
