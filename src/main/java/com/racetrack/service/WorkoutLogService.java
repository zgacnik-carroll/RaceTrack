package com.racetrack.service;

import com.racetrack.model.WorkoutLog;
import com.racetrack.repository.WorkoutLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(WorkoutLogService.class);
    private static final int TEXT_MAX = 2000;

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
        if (selectedDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date is required.");
        }
        validateWorkoutFields(
                workoutLog.getWorkoutType(),
                workoutLog.getCompletionDetails(),
                workoutLog.getActualPaces(),
                workoutLog.getWorkoutDescription()
        );

        workoutLog.setWorkoutType(normalizeWorkoutType(workoutLog.getWorkoutType()));
        workoutLog.setCompletionDetails(normalizeOptionalText(workoutLog.getCompletionDetails(), "Completion details"));
        workoutLog.setActualPaces(normalizeOptionalText(workoutLog.getActualPaces(), "Actual paces"));
        workoutLog.setWorkoutDescription(normalizeOptionalText(workoutLog.getWorkoutDescription(), "Workout description"));

        if (selectedDate != null) {
            workoutLog.setLogDate(selectedDate.atStartOfDay());
        } else if (workoutLog.getLogDate() == null) {
            workoutLog.setLogDate(LocalDate.now().atStartOfDay());
        }
        WorkoutLog savedLog = workoutLogRepository.save(workoutLog);
        log.info("Workout log saved logId={} userId={} logDate={}",
                savedLog.getId(), savedLog.getUser() != null ? savedLog.getUser().getId() : null, savedLog.getLogDate());
        return savedLog;
    }

    /**
     * Returns all workout logs for a user.
     *
     * @param userId user id owner
     * @return workout logs ordered newest first
     */
    public List<WorkoutLog> findByUserId(String userId) {
        List<WorkoutLog> logs = workoutLogRepository.findByUser_IdOrderByLogDateDesc(userId);
        log.info("Workout logs loaded userId={} count={}", userId, logs.size());
        return logs;
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
        WorkoutLog workoutLog = workoutLogRepository.findByIdAndUser_Id(logId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log not found."));

        validateWorkoutFields(workoutType, completionDetails, actualPaces, workoutDescription);

        workoutLog.setWorkoutType(normalizeWorkoutType(workoutType));
        workoutLog.setCompletionDetails(normalizeOptionalText(completionDetails, "Completion details"));
        workoutLog.setActualPaces(normalizeOptionalText(actualPaces, "Actual paces"));
        workoutLog.setWorkoutDescription(normalizeOptionalText(workoutDescription, "Workout description"));
        if (logDate != null) {
            workoutLog.setLogDate(logDate.atStartOfDay());
        }
        WorkoutLog savedLog = workoutLogRepository.save(workoutLog);
        log.info("Workout log updated logId={} userId={} logDate={}", savedLog.getId(), userId, savedLog.getLogDate());
        return savedLog;
    }

    /**
     * Deletes a workout log when it belongs to the authenticated athlete.
     *
     * @param userId owner user id
     * @param logId workout log id
     */
    public void deleteAthleteOwnedLog(String userId, Long logId) {
        WorkoutLog workoutLog = workoutLogRepository.findByIdAndUser_Id(logId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log not found."));
        workoutLogRepository.delete(workoutLog);
        log.info("Workout log deleted logId={} userId={}", logId, userId);
    }

    /**
     * Updates coach comment on a workout log.
     *
     * @param logId workout log id
     * @param coachComment coach comment content
     * @return saved workout log
     */
    public WorkoutLog updateCoachComment(Long logId, String coachComment) {
        WorkoutLog workoutLog = workoutLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log not found."));
        String normalized = normalizeOptionalText(coachComment, "Coach comment");
        workoutLog.setCoachComment(normalized);
        WorkoutLog savedLog = workoutLogRepository.save(workoutLog);
        log.info("Workout coach comment saved logId={} hasComment={}", logId, normalized != null);
        return savedLog;
    }

    /**
     * Enforces core workout-field rules before persistence.
     *
     * @param workoutType workout type text
     * @param completionDetails completion details text
     * @param actualPaces actual paces text
     * @param workoutDescription workout description text
     */
    private void validateWorkoutFields(String workoutType,
                                       String completionDetails,
                                       String actualPaces,
                                       String workoutDescription) {
        normalizeWorkoutType(workoutType);
        normalizeRequiredText(completionDetails, "Completion details");
        normalizeRequiredText(actualPaces, "Actual paces");
        normalizeRequiredText(workoutDescription, "Workout description");
    }

    /**
     * Normalizes and validates workout type values.
     *
     * @param workoutType raw workout type
     * @return normalized workout type
     */
    private String normalizeWorkoutType(String workoutType) {
        String normalized = normalizeRequiredText(workoutType, "Workout type");
        if (!normalized.equals("Strength")
                && !normalized.equals("Strides")
                && !normalized.equals("Workout")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workout type must be Strength, Strides, or Workout.");
        }
        return normalized;
    }

    /**
     * Normalizes optional text inputs so empty strings are stored as null.
     *
     * @param text raw text
     * @param fieldName field label for error messages
     * @return trimmed text or null
     */
    private String normalizeOptionalText(String text, String fieldName) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.length() > TEXT_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " cannot exceed " + TEXT_MAX + " characters.");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Normalizes required text inputs and rejects null/blank values.
     *
     * @param text raw text
     * @param fieldName field label for error messages
     * @return trimmed, non-empty text
     */
    private String normalizeRequiredText(String text, String fieldName) {
        String normalized = normalizeOptionalText(text, fieldName);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        return normalized;
    }
}
