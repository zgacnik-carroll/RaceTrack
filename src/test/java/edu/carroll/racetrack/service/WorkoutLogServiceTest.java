package edu.carroll.racetrack.service;

import edu.carroll.racetrack.model.User;
import edu.carroll.racetrack.model.WorkoutLog;
import edu.carroll.racetrack.repository.UserRepository;
import edu.carroll.racetrack.repository.WorkoutLogRepository;
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
 * Unit tests for {@link WorkoutLogService} backed by in-memory H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkoutLogServiceTest {

    @Autowired
    private WorkoutLogService workoutLogService;

    @Autowired
    private WorkoutLogRepository workoutLogRepository;

    @Autowired
    private UserRepository userRepository;

    // -------------------------------------------------------------------------
    // submitWorkoutLog
    // -------------------------------------------------------------------------

    @Test
    void submitWorkoutLog_setsSelectedDateAtStartOfDay() {
        User user = userRepository.save(user("w1@example.com"));
        WorkoutLog log = workoutLog(user, "Workout", "8x400");

        WorkoutLog saved = workoutLogService.submitWorkoutLog(log, LocalDate.of(2026, 3, 1));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLogDate()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
    }

    @Test
    void submitWorkoutLog_rejectsNullDate() {
        User user = userRepository.save(user("w1b@example.com"));
        WorkoutLog log = workoutLog(user, "Strength", "Core circuit");

        assertThatThrownBy(() -> workoutLogService.submitWorkoutLog(log, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Date is required.")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void submitWorkoutLog_persistsAllSuppliedFields() {
        User user = userRepository.save(user("w1c@example.com"));
        WorkoutLog log = new WorkoutLog();
        log.setUser(user);
        log.setWorkoutType("Workout");
        log.setCompletionDetails("6x800 at threshold");
        log.setActualPaces("3:02, 3:05, 3:04, 3:06, 3:03, 3:07");
        log.setWorkoutDescription("Lactate threshold session on the track");

        WorkoutLog saved = workoutLogService.submitWorkoutLog(log, LocalDate.of(2026, 3, 10));

        assertThat(saved.getWorkoutType()).isEqualTo("Workout");
        assertThat(saved.getCompletionDetails()).isEqualTo("6x800 at threshold");
        assertThat(saved.getActualPaces()).isEqualTo("3:02, 3:05, 3:04, 3:06, 3:03, 3:07");
        assertThat(saved.getWorkoutDescription()).isEqualTo("Lactate threshold session on the track");
        assertThat(saved.getLogDate()).isEqualTo(LocalDateTime.of(2026, 3, 10, 0, 0));
    }

    // -------------------------------------------------------------------------
    // findByUserId
    // -------------------------------------------------------------------------

    @Test
    void findByUserId_returnsNewestFirst() {
        User user = userRepository.save(user("w2@example.com"));
        WorkoutLog older = workoutLog(user, "Strength", "Core");
        older.setLogDate(LocalDateTime.of(2026, 2, 1, 0, 0));
        WorkoutLog newer = workoutLog(user, "Workout", "Tempo");
        newer.setLogDate(LocalDateTime.of(2026, 3, 1, 0, 0));
        workoutLogRepository.save(older);
        workoutLogRepository.save(newer);

        List<WorkoutLog> logs = workoutLogService.findByUserId(user.getId());

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).getCompletionDetails()).isEqualTo("Tempo");
        assertThat(logs.get(1).getCompletionDetails()).isEqualTo("Core");
    }

    @Test
    void findByUserId_returnsEmptyListWhenUserHasNoLogs() {
        User user = userRepository.save(user("w2b@example.com"));

        List<WorkoutLog> logs = workoutLogService.findByUserId(user.getId());

        assertThat(logs).isEmpty();
    }

    @Test
    void findByUserId_doesNotReturnLogsOwnedByOtherUsers() {
        User userA = userRepository.save(user("w2c@example.com"));
        User userB = userRepository.save(user("w2d@example.com"));
        workoutLogRepository.save(workoutLog(userA, "Strides", "User A strides"));
        workoutLogRepository.save(workoutLog(userB, "Strength", "User B core"));

        List<WorkoutLog> logs = workoutLogService.findByUserId(userA.getId());

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getCompletionDetails()).isEqualTo("User A strides");
    }

    // -------------------------------------------------------------------------
    // updateAthleteOwnedLog
    // -------------------------------------------------------------------------

    @Test
    void updateAthleteOwnedLog_updatesFieldsWhenOwnedByUser() {
        User user = userRepository.save(user("w3@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(user, "Strength", "Before"));

        WorkoutLog updated = workoutLogService.updateAthleteOwnedLog(
                user.getId(),
                log.getId(),
                "Workout",
                "After completion",
                "5:30/mi",
                "Threshold session",
                LocalDate.of(2026, 3, 2)
        );

        assertThat(updated.getWorkoutType()).isEqualTo("Workout");
        assertThat(updated.getCompletionDetails()).isEqualTo("After completion");
        assertThat(updated.getActualPaces()).isEqualTo("5:30/mi");
        assertThat(updated.getWorkoutDescription()).isEqualTo("Threshold session");
        assertThat(updated.getLogDate()).isEqualTo(LocalDateTime.of(2026, 3, 2, 0, 0));
    }

    @Test
    void updateAthleteOwnedLog_doesNotChangeDateWhenNullDateProvided() {
        User user = userRepository.save(user("w3b@example.com"));
        WorkoutLog log = workoutLog(user, "Strides", "Original");
        log.setLogDate(LocalDateTime.of(2026, 1, 15, 0, 0));
        log = workoutLogRepository.save(log);

        WorkoutLog updated = workoutLogService.updateAthleteOwnedLog(
                user.getId(), log.getId(),
                "Strides", "Updated details", "27s per 100m", "Post-run strides", null
        );

        assertThat(updated.getLogDate()).isEqualTo(LocalDateTime.of(2026, 1, 15, 0, 0));
    }

    @Test
    void updateAthleteOwnedLog_throwsNotFoundWhenNotOwned() {
        assertThatThrownBy(() -> workoutLogService.updateAthleteOwnedLog(
                999L,
                999L,
                "Workout",
                "x",
                "y",
                "z",
                null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Workout log not found.")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAthleteOwnedLog_throwsNotFoundWhenLogExistsButBelongsToDifferentUser() {
        User owner = userRepository.save(user("w3c@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(owner, "Workout", "Owner's log"));

        assertThatThrownBy(() -> workoutLogService.updateAthleteOwnedLog(
                1000L, log.getId(),
                "Workout", "hack attempt", "", "", null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAthleteOwnedLog_rejectsInvalidWorkoutType() {
        User user = userRepository.save(user("w3d@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(user, "Workout", "Before"));

        assertThatThrownBy(() -> workoutLogService.updateAthleteOwnedLog(
                user.getId(), log.getId(),
                "Cycling", "Updated details", "27s per 100m", "Post-run strides", null
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
        User user = userRepository.save(user("w4@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(user, "Strength", "Delete me"));

        workoutLogService.deleteAthleteOwnedLog(user.getId(), log.getId());

        assertThat(workoutLogRepository.findById(log.getId())).isEmpty();
    }

    @Test
    void deleteAthleteOwnedLog_throwsNotFoundWhenLogDoesNotExist() {
        assertThatThrownBy(() -> workoutLogService.deleteAthleteOwnedLog(13L, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Workout log not found.")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteAthleteOwnedLog_throwsNotFoundWhenLogBelongsToDifferentUser() {
        User owner = userRepository.save(user("w4c@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(owner, "Workout", "Not yours"));

        assertThatThrownBy(() -> workoutLogService.deleteAthleteOwnedLog(1001L, log.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteAthleteOwnedLog_onlyDeletesTargetLog() {
        User user = userRepository.save(user("w4d@example.com"));
        WorkoutLog toDelete = workoutLogRepository.save(workoutLog(user, "Strength", "Delete me"));
        WorkoutLog toKeep   = workoutLogRepository.save(workoutLog(user, "Workout",  "Keep me"));

        workoutLogService.deleteAthleteOwnedLog(user.getId(), toDelete.getId());

        assertThat(workoutLogRepository.findById(toDelete.getId())).isEmpty();
        assertThat(workoutLogRepository.findById(toKeep.getId())).isPresent();
    }

    // -------------------------------------------------------------------------
    // updateCoachComment
    // -------------------------------------------------------------------------

    @Test
    void updateCoachComment_trimsAndNullsBlank() {
        User user = userRepository.save(user("w5@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(user, "Workout", "Comment target"));

        WorkoutLog trimmed = workoutLogService.updateCoachComment(log.getId(), "  Strong day  ");
        assertThat(trimmed.getCoachComment()).isEqualTo("Strong day");

        WorkoutLog blank = workoutLogService.updateCoachComment(log.getId(), "   ");
        assertThat(blank.getCoachComment()).isNull();
    }

    @Test
    void updateCoachComment_storesNullWhenNullPassed() {
        User user = userRepository.save(user("w5b@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(user, "Strides", "Null comment"));

        WorkoutLog updated = workoutLogService.updateCoachComment(log.getId(), null);

        assertThat(updated.getCoachComment()).isNull();
    }

    @Test
    void updateCoachComment_overwritesPreviousComment() {
        User user = userRepository.save(user("w5c@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(user, "Workout", "Has comment"));

        workoutLogService.updateCoachComment(log.getId(), "First comment");
        WorkoutLog updated = workoutLogService.updateCoachComment(log.getId(), "Second comment");

        assertThat(updated.getCoachComment()).isEqualTo("Second comment");
    }

    @Test
    void updateCoachComment_throwsNotFoundForUnknownLogId() {
        assertThatThrownBy(() -> workoutLogService.updateCoachComment(999L, "Great effort"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Workout log not found.")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateCoachComment_rejectsTextOver2000Characters() {
        User user = userRepository.save(user("w5d@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(user, "Workout", "Comment target"));
        String tooLong = "x".repeat(2001);

        assertThatThrownBy(() -> workoutLogService.updateCoachComment(log.getId(), tooLong))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setRole("athlete");
        return user;
    }

    private WorkoutLog workoutLog(User user, String type, String completionDetails) {
        WorkoutLog log = new WorkoutLog();
        log.setUser(user);
        log.setWorkoutType(type);
        log.setCompletionDetails(completionDetails);
        log.setActualPaces("Steady");
        log.setWorkoutDescription("Workout details");
        return log;
    }
}

