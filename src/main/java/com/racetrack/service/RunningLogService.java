package com.racetrack.service;

import com.racetrack.model.RunningLog;
import com.racetrack.repository.RunningLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Running log operations grouped by model-level behavior.
 */
@Service
public class RunningLogService {
    private static final Logger log = LoggerFactory.getLogger(RunningLogService.class);
    private static final int TEXT_MAX = 2000;
    private static final int FEEL_MAX = 100;
    private static final int PAIN_DETAILS_MAX = 100;

    private final RunningLogRepository runningLogRepository;

    /**
     * Creates a running-log service.
     *
     * @param runningLogRepository persistent store for running logs
     */
    public RunningLogService(RunningLogRepository runningLogRepository) {
        this.runningLogRepository = runningLogRepository;
    }

    /**
     * Persists a newly submitted running log.
     *
     * @param runningLog running log model
     * @param selectedDate optional date supplied from form
     * @return saved running log
     */
    public RunningLog submitRunningLog(RunningLog runningLog, LocalDate selectedDate) {
        if (selectedDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date is required.");
        }
        validateRunningFields(
                runningLog.getMileage(),
                runningLog.getHurting(),
                runningLog.getPainDetails(),
                runningLog.getSleepHours(),
                runningLog.getStressLevel(),
                runningLog.getPlateProportion(),
                runningLog.getGotThatBread(),
                runningLog.getFeel(),
                runningLog.getRpe(),
                runningLog.getDetails()
        );

        // Persist normalized text so spreadsheet edits and form submissions share the same storage rules.
        runningLog.setFeel(normalizeRequiredText(runningLog.getFeel(), "Feel", FEEL_MAX));
        runningLog.setPainDetails(normalizePainDetails(runningLog.getHurting(), runningLog.getPainDetails()));
        runningLog.setDetails(normalizeRequiredText(runningLog.getDetails(), "Details"));

        if (selectedDate != null) {
            runningLog.setLogDate(selectedDate.atStartOfDay());
        } else if (runningLog.getLogDate() == null) {
            runningLog.setLogDate(LocalDate.now().atStartOfDay());
        }
        RunningLog savedLog = runningLogRepository.save(runningLog);
        log.info("Running log saved logId={} userId={} logDate={}",
                savedLog.getId(), savedLog.getUser() != null ? savedLog.getUser().getId() : null, savedLog.getLogDate());
        return savedLog;
    }

    /**
     * Returns all running logs for a user.
     *
     * @param userId user id owner
     * @return running logs ordered newest first
     */
    public List<RunningLog> findByUserId(String userId) {
        List<RunningLog> logs = runningLogRepository.findByUser_IdOrderByLogDateDesc(userId);
        log.info("Running logs loaded userId={} count={}", userId, logs.size());
        return logs;
    }

    /**
     * Updates a running log that belongs to the authenticated athlete.
     *
     * @param userId owner user id
     * @param logId running log id
     * @param mileage mileage value
     * @param hurting hurting flag
     * @param sleepHours sleep hours
     * @param stressLevel stress level
     * @param plateProportion nutrition plate flag
     * @param gotThatBread nutrition carb flag
     * @param feel self-reported feel value
     * @param rpe effort rating
     * @param details athlete details text
     * @param logDate optional log date override
     * @return saved running log
     */
    public RunningLog updateAthleteOwnedLog(String userId,
                                            Long logId,
                                            Double mileage,
                                            Boolean hurting,
                                            String painDetails,
                                            Double sleepHours,
                                            Integer stressLevel,
                                            Boolean plateProportion,
                                            Boolean gotThatBread,
                                            String feel,
                                            Integer rpe,
                                            String details,
                                            LocalDate logDate) {
        RunningLog runningLog = runningLogRepository.findByIdAndUser_Id(logId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Running log not found."));

        // Reuse the same validation rules as form submission so row editing cannot bypass constraints.
        validateRunningFields(mileage, hurting, painDetails, sleepHours, stressLevel, plateProportion, gotThatBread, feel, rpe, details);

        runningLog.setMileage(mileage);
        runningLog.setHurting(hurting);
        runningLog.setPainDetails(normalizePainDetails(hurting, painDetails));
        runningLog.setSleepHours(sleepHours);
        runningLog.setStressLevel(stressLevel);
        runningLog.setPlateProportion(plateProportion);
        runningLog.setGotThatBread(gotThatBread);
        runningLog.setFeel(normalizeRequiredText(feel, "Feel", FEEL_MAX));
        runningLog.setRpe(rpe);
        runningLog.setDetails(normalizeRequiredText(details, "Details"));
        if (logDate != null) {
            runningLog.setLogDate(logDate.atStartOfDay());
        }
        RunningLog savedLog = runningLogRepository.save(runningLog);
        log.info("Running log updated logId={} userId={} logDate={}", savedLog.getId(), userId, savedLog.getLogDate());
        return savedLog;
    }

    /**
     * Deletes a running log when it belongs to the authenticated athlete.
     *
     * @param userId owner user id
     * @param logId running log id
     */
    public void deleteAthleteOwnedLog(String userId, Long logId) {
        RunningLog runningLog = runningLogRepository.findByIdAndUser_Id(logId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Running log not found."));
        runningLogRepository.delete(runningLog);
        log.info("Running log deleted logId={} userId={}", logId, userId);
    }

    /**
     * Updates coach comment on a running log.
     *
     * @param logId running log id
     * @param coachComment coach comment content
     * @return saved running log
     */
    public RunningLog updateCoachComment(Long logId, String coachComment) {
        RunningLog runningLog = runningLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Running log not found."));
        String normalized = normalizeOptionalText(coachComment, "Coach comment");
        runningLog.setCoachComment(normalized);
        RunningLog savedLog = runningLogRepository.save(runningLog);
        log.info("Running coach comment saved logId={} hasComment={}", logId, normalized != null);
        return savedLog;
    }

    /**
     * Enforces rules for running-log numeric and text values.
     *
     * @param mileage mileage value
     * @param sleepHours sleep value
     * @param stressLevel stress value
     * @param feel feel value
     * @param rpe rpe value
     * @param details details value
     */
    private void validateRunningFields(Double mileage,
                                       Boolean hurting,
                                       String painDetails,
                                       Double sleepHours,
                                       Integer stressLevel,
                                       Boolean plateProportion,
                                       Boolean gotThatBread,
                                       String feel,
                                       Integer rpe,
                                       String details) {
        if (mileage == null || mileage < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mileage must be 0 or greater.");
        }
        if (hurting == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hurting is required.");
        }
        normalizePainDetails(hurting, painDetails);
        if (sleepHours == null || sleepHours < 0 || sleepHours > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sleep must be between 0 and 24.");
        }
        if (stressLevel == null || stressLevel < 1 || stressLevel > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stress must be between 1 and 10.");
        }
        if (plateProportion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plate proportions is required.");
        }
        if (gotThatBread == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Did you get that bread is required.");
        }
        if (rpe == null || rpe < 0 || rpe > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RPE must be between 0 and 10.");
        }

        // Validate required freeform fields after numeric and boolean guardrails have passed.
        normalizeRequiredText(feel, "Feel", FEEL_MAX);
        normalizeRequiredText(details, "Details");
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

    /**
     * Normalizes required text inputs with a custom max length.
     *
     * @param text raw text
     * @param fieldName field label for error messages
     * @param maxLength maximum allowed length
     * @return trimmed, non-empty text
     */
    private String normalizeRequiredText(String text, String fieldName, int maxLength) {
        if (text == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        if (trimmed.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " cannot exceed " + maxLength + " characters.");
        }
        return trimmed;
    }

    /**
     * Normalizes hurting details based on hurting selection.
     *
     * @param hurting hurting flag
     * @param painDetails hurting details text
     * @return trimmed hurting details or null when not hurting
     */
    private String normalizePainDetails(Boolean hurting, String painDetails) {
        if (Boolean.FALSE.equals(hurting)) {
            return null;
        }
        return normalizeRequiredText(painDetails, "Hurting details", PAIN_DETAILS_MAX);
    }
}
