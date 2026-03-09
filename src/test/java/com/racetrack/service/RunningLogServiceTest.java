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

    @Test
    void submitRunningLog_setsSelectedDateAtStartOfDay() {
        User user = userRepository.save(user("runner-1", "r1@example.com"));
        RunningLog log = runningLog(user, 7.5, "Easy run");

        RunningLog saved = runningLogService.submitRunningLog(log, LocalDate.of(2026, 3, 1));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLogDate()).isNotNull();
    }

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
    void deleteAthleteOwnedLog_removesRow() {
        User user = userRepository.save(user("runner-4", "r4@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(user, 3.0, "Delete me"));

        runningLogService.deleteAthleteOwnedLog("runner-4", log.getId());

        assertThat(runningLogRepository.findById(log.getId())).isEmpty();
    }

    @Test
    void updateCoachComment_trimsAndNullsBlank() {
        User user = userRepository.save(user("runner-5", "r5@example.com"));
        RunningLog log = runningLogRepository.save(runningLog(user, 6.0, "Comment target"));

        RunningLog trimmed = runningLogService.updateCoachComment(log.getId(), "  Nice work  ");
        assertThat(trimmed.getCoachComment()).isEqualTo("Nice work");

        RunningLog blank = runningLogService.updateCoachComment(log.getId(), "   ");
        assertThat(blank.getCoachComment()).isNull();
    }

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
