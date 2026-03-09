package com.racetrack.service;

import com.racetrack.model.RunningLog;
import com.racetrack.repository.RunningLogRepository;
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
        if (selectedDate != null) {
            runningLog.setLogDate(selectedDate.atStartOfDay());
        }
        return runningLogRepository.save(runningLog);
    }

    /**
     * Returns all running logs for a user.
     *
     * @param userId user id owner
     * @return running logs ordered newest first
     */
    public List<RunningLog> findByUserId(String userId) {
        return runningLogRepository.findByUser_IdOrderByLogDateDesc(userId);
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
                                            Integer sleepHours,
                                            Integer stressLevel,
                                            Boolean plateProportion,
                                            Boolean gotThatBread,
                                            String feel,
                                            Integer rpe,
                                            String details,
                                            LocalDate logDate) {
        RunningLog log = runningLogRepository.findByIdAndUser_Id(logId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Running log not found."));

        log.setMileage(mileage);
        log.setHurting(hurting);
        log.setSleepHours(sleepHours);
        log.setStressLevel(stressLevel);
        log.setPlateProportion(plateProportion);
        log.setGotThatBread(gotThatBread);
        log.setFeel(feel);
        log.setRpe(rpe);
        log.setDetails(details);
        if (logDate != null) {
            log.setLogDate(logDate.atStartOfDay());
        }
        return runningLogRepository.save(log);
    }

    /**
     * Deletes a running log when it belongs to the authenticated athlete.
     *
     * @param userId owner user id
     * @param logId running log id
     */
    public void deleteAthleteOwnedLog(String userId, Long logId) {
        RunningLog log = runningLogRepository.findByIdAndUser_Id(logId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Running log not found."));
        runningLogRepository.delete(log);
    }

    /**
     * Updates coach comment on a running log.
     *
     * @param logId running log id
     * @param coachComment coach comment content
     * @return saved running log
     */
    public RunningLog updateCoachComment(Long logId, String coachComment) {
        RunningLog log = runningLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Running log not found."));
        log.setCoachComment(normalizeText(coachComment));
        return runningLogRepository.save(log);
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
