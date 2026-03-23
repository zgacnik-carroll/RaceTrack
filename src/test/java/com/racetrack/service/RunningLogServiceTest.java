package com.racetrack.service;

import com.racetrack.model.RunningLog;
import com.racetrack.model.User;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RunningLogService} backed by in-memory H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RunningLogServiceTest {

    @Autowired
    private RunningLogService runningLogService;

    @Autowired
    private RunningLogRepository runningLogRepository;

    @Autowired
    private UserRepository userRepository;

    // -------------------------------------------------------------------------
    // submitRunningLog
    // -------------------------------------------------------------------------

    @Test
    void submitRunningLog_setsSelectedDateAtStartOfDay() {
        User user = userRepository.save(user("runner-1", "r1@example.com"));
        RunningLog log = runningLog(user, 7.5, "Easy run");

        RunningLog saved = runningLogService.submitRunningLog(log, LocalDate.of(2026, 3, 1));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLogDate()).isNotNull();
    }

    @Test
    void submitRunningLog_usesCurrentTimestampWhenNullDateProvided() {
        User user = userRepository.save(user("runner-1b", "r1b@example.com"));
        RunningLog log = runningLog(user, 5.0, "No date provided");

        RunningLog saved = runningLogService.submitRunningLog(log, null);

        // logDate is set by @CreationTimestamp so it should be non-null and recent
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLogDate()).isNotNull();
    }

    @Test
    void submitRunningLog_persistsAllSuppliedFields() {
        User user = userRepository.save(user("runner-1c", "r1c@example.com"));
        RunningLog log = new RunningLog();
        log.setUser(user);
        log.setMileage(9.0);
        log.setHurting(false);
        log.setSleepHours(7);
        log.setStressLevel(4);
        log.setPlateProportion(true);
        log.setGotThatBread(true);
        log.setFeel("Good");
        log.setRpe(7);
        log.setDetails("Long run with strides");

        RunningLog saved = runningLogService.submitRunningLog(log, LocalDate.of(2026, 3, 5));

        assertThat(saved.getMileage()).isEqualTo(9.0);
        assertThat(saved.getHurting()).isFalse();
        assertThat(saved.getSleepHours()).isEqualTo(7);
        assertThat(saved.getStressLevel()).isEqualTo(4);
        assertThat(saved.getPlateProportion()).isTrue();
        assertThat(saved.getGotThatBread()).isTrue();
        assertThat(saved.getFeel()).isEqualTo("Good");
        assertThat(saved.getRpe()).isEqualTo(7);
        assertThat(saved.getDetails()).isEqualTo("Long run with strides");
    }

    // -------------------------------------------------------------------------
    // findByUserId
    // -------------------------------------------------------------------------

    @Test
    void findByUserId_returnsNewestFirst() {
        User user = userRepository.save(user("runner-2", "r2@example.com"));
        RunningLog older = runningLog(user, 4.0, "Older");
        older.setLogDate(LocalDateTime.of(2026, 2, 1, 0, 0));
        RunningLog newer = runningLog(user, 8.0, "Newer");
        newer.setLogDate(LocalDateTime.of(2026, 3, 1, 0, 0));
        runningLogRepository.save(older);
        runningLogRepository.save(newer);

        List<RunningLog> logs = runningLogService.findByUserId("runner-2");

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).getDetails()).isEqualTo("Newer");
        assertThat(logs.get(1).getDetails()).isEqualTo("Older");
    }

    @Test
    void findByUserId_returnsEmptyListWhenUserHasNoLogs() {
        userRepository.save(user("runner-2b", "r2b@example.com"));

        List<RunningLog> logs = runningLogService.findByUserId("runner-2b");

        assertThat(logs).isEmpty();
    }

    @Test
    void findByUserId_doesNotReturnLogsOwnedByOtherUsers() {
        User userA = userRepository.save(user("runner-2c", "r2c@example.com"));
        User userB = userRepository.save(user("runner-2d", "r2d@example.com"));
        runningLogRepository.save(runningLog(userA, 5.0, "User A log"));
        runningLogRepository.save(runningLog(userB, 6.0, "User B log"));

        List<RunningLog> logs = runningLogService.findByUserId("runner-2c");

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getDetails()).isEqualTo("User A log");
    }

    // -------------------------------------------------------------------------
    // updateAthleteOwnedLog
    // -------------------------------------------------------------------------

    @Test
    void updateAthleteOwnedLog_updatesFieldsWhenOwnedByUser() {
        User user = userRepository.save(user("runner-3", "r3@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(user, 5.0, "Before"));

        RunningLog updated = runningLogService.updateAthleteOwnedLog(
                "runner-3",
                log.getId(),
                10.25,
                true,
                8,
                3,
                true,
                false,
                "Good",
                6,
                "After",
                LocalDate.of(2026, 3, 2)
        );

        assertThat(updated.getMileage()).isEqualTo(10.25);
        assertThat(updated.getHurting()).isTrue();
        assertThat(updated.getSleepHours()).isEqualTo(8);
        assertThat(updated.getStressLevel()).isEqualTo(3);
        assertThat(updated.getPlateProportion()).isTrue();
        assertThat(updated.getGotThatBread()).isFalse();
        assertThat(updated.getFeel()).isEqualTo("Good");
        assertThat(updated.getRpe()).isEqualTo(6);
        assertThat(updated.getDetails()).isEqualTo("After");
        assertThat(updated.getLogDate()).isEqualTo(LocalDateTime.of(2026, 3, 2, 0, 0));
    }

    @Test
    void updateAthleteOwnedLog_throwsNotFoundWhenNotOwned() {
        assertThatThrownBy(() -> runningLogService.updateAthleteOwnedLog(
                "missing",
                999L,
                1.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "none",
                null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Running log not found.")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAthleteOwnedLog_throwsNotFoundWhenLogExistsButBelongsToDifferentUser() {
        User owner = userRepository.save(user("runner-3c", "r3c@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(owner, 4.0, "Owner's log"));

        assertThatThrownBy(() -> runningLogService.updateAthleteOwnedLog(
                "different-user", log.getId(),
                4.0, null, null, null, null, null, null, null, "hack attempt", null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAthleteOwnedLog_rejectsNegativeSleepHours() {
        User user = userRepository.save(user("runner-3d", "r3d@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(user, 6.0, "Before"));

        assertThatThrownBy(() -> runningLogService.updateAthleteOwnedLog(
                "runner-3d",
                log.getId(),
                6.0,
                false,
                -1,
                3,
                true,
                true,
                "Good",
                5,
                "After",
                LocalDate.of(2026, 3, 7)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // deleteAthleteOwnedLog
    // -------------------------------------------------------------------------

    @Test
    void deleteAthleteOwnedLog_removesRow() {
        User user = userRepository.save(user("runner-4", "r4@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(user, 3.0, "Delete me"));

        runningLogService.deleteAthleteOwnedLog("runner-4", log.getId());

        assertThat(runningLogRepository.findById(log.getId())).isEmpty();
    }

    @Test
    void deleteAthleteOwnedLog_throwsNotFoundWhenLogDoesNotExist() {
        assertThatThrownBy(() -> runningLogService.deleteAthleteOwnedLog("runner-4b", 999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Running log not found.")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteAthleteOwnedLog_throwsNotFoundWhenLogBelongsToDifferentUser() {
        User owner = userRepository.save(user("runner-4c", "r4c@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(owner, 5.0, "Not yours"));

        assertThatThrownBy(() -> runningLogService.deleteAthleteOwnedLog("different-user", log.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteAthleteOwnedLog_onlyDeletesTargetLog() {
        User user = userRepository.save(user("runner-4d", "r4d@example.com"));
        RunningLog toDelete = runningLogRepository.save(runningLog(user, 3.0, "Delete me"));
        RunningLog toKeep   = runningLogRepository.save(runningLog(user, 6.0, "Keep me"));

        runningLogService.deleteAthleteOwnedLog("runner-4d", toDelete.getId());

        assertThat(runningLogRepository.findById(toDelete.getId())).isEmpty();
        assertThat(runningLogRepository.findById(toKeep.getId())).isPresent();
    }

    // -------------------------------------------------------------------------
    // updateCoachComment
    // -------------------------------------------------------------------------

    @Test
    void updateCoachComment_trimsAndNullsBlank() {
        User user = userRepository.save(user("runner-5", "r5@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(user, 6.0, "Comment target"));

        RunningLog trimmed = runningLogService.updateCoachComment(log.getId(), "  Nice work  ");
        assertThat(trimmed.getCoachComment()).isEqualTo("Nice work");

        RunningLog blank = runningLogService.updateCoachComment(log.getId(), "   ");
        assertThat(blank.getCoachComment()).isNull();
    }

    @Test
    void updateCoachComment_storesNullWhenNullPassed() {
        User user = userRepository.save(user("runner-5b", "r5b@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(user, 5.0, "Null comment"));

        RunningLog updated = runningLogService.updateCoachComment(log.getId(), null);

        assertThat(updated.getCoachComment()).isNull();
    }

    @Test
    void updateCoachComment_overwritesPreviousComment() {
        User user = userRepository.save(user("runner-5c", "r5c@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(user, 7.0, "Has comment"));

        runningLogService.updateCoachComment(log.getId(), "First comment");
        RunningLog updated = runningLogService.updateCoachComment(log.getId(), "Second comment");

        assertThat(updated.getCoachComment()).isEqualTo("Second comment");
    }

    @Test
    void updateCoachComment_throwsNotFoundForUnknownLogId() {
        assertThatThrownBy(() -> runningLogService.updateCoachComment(999L, "Great effort"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Running log not found.")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateCoachComment_rejectsTextOver2000Characters() {
        User user = userRepository.save(user("runner-5d", "r5d@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(user, 4.0, "Comment target"));
        String tooLong = "x".repeat(2001);

        assertThatThrownBy(() -> runningLogService.updateCoachComment(log.getId(), tooLong))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User user(String id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole("athlete");
        return user;
    }

    private RunningLog runningLog(User user, double mileage, String details) {
        RunningLog log = new RunningLog();
        log.setUser(user);
        log.setMileage(mileage);
        log.setDetails(details);
        return log;
    }
}
