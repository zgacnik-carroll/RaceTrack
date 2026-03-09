package com.racetrack.service;

import com.racetrack.model.WorkoutLog;
import com.racetrack.repository.WorkoutLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Workout log operations grouped by model-level behavior.
 */
@Service
public class WorkoutLogService {

    private final WorkoutLogRepository workoutLogRepository;

    /**
     * Creates a workout-log service.
     *
     * @param workoutLogRepository persistent store for workout logs
     */
    public WorkoutLogService(WorkoutLogRepository workoutLogRepository) {
        this.workoutLogRepository = workoutLogRepository;
    }

    /**
     * Persists a newly submitted workout log.
     *
     * @param workoutLog workout log model
     * @param selectedDate optional date supplied from form
     * @return saved workout log
     */
    public WorkoutLog submitWorkoutLog(WorkoutLog workoutLog, LocalDate selectedDate) {
        if (selectedDate != null) {
            workoutLog.setLogDate(selectedDate.atStartOfDay());
        }
        return workoutLogRepository.save(workoutLog);
    }

    /**
     * Returns all workout logs for a user.
     *
     * @param userId user id owner
     * @return workout logs ordered newest first
     */
    public List<WorkoutLog> findByUserId(String userId) {
        return workoutLogRepository.findByUser_IdOrderByLogDateDesc(userId);
    }

    /**
     * Updates a workout log that belongs to the authenticated athlete.
     *
     * @param userId owner user id
     * @param logId workout log id
     * @param workoutType workout type text
     * @param completionDetails completion details text
     * @param actualPaces actual paces text
     * @param workoutDescription workout description text
     * @param logDate optional log date override
     * @return saved workout log
     */
    public WorkoutLog updateAthleteOwnedLog(String userId,
                                            Long logId,
                                            String workoutType,
                                            String completionDetails,
                                            String actualPaces,
                                            String workoutDescription,
                                            LocalDate logDate) {
        WorkoutLog log = workoutLogRepository.findByIdAndUser_Id(logId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log not found."));

        log.setWorkoutType(workoutType);
        log.setCompletionDetails(completionDetails);
        log.setActualPaces(actualPaces);
        log.setWorkoutDescription(workoutDescription);
        if (logDate != null) {
            log.setLogDate(logDate.atStartOfDay());
        }
        return workoutLogRepository.save(log);
    }

    /**
     * Deletes a workout log when it belongs to the authenticated athlete.
     *
     * @param userId owner user id
     * @param logId workout log id
     */
    public void deleteAthleteOwnedLog(String userId, Long logId) {
        WorkoutLog log = workoutLogRepository.findByIdAndUser_Id(logId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log not found."));
        workoutLogRepository.delete(log);
    }

    /**
     * Updates coach comment on a workout log.
     *
     * @param logId workout log id
     * @param coachComment coach comment content
     * @return saved workout log
     */
    public WorkoutLog updateCoachComment(Long logId, String coachComment) {
        WorkoutLog log = workoutLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log not found."));
        log.setCoachComment(normalizeText(coachComment));
        return workoutLogRepository.save(log);
    }

    /**
     * Normalizes text inputs so empty strings are stored as null.
     *
     * @param text raw text
     * @return trimmed text or null
     */
    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
