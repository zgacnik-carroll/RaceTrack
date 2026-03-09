package com.racetrack.service;

import com.racetrack.model.User;
import com.racetrack.model.WorkoutLog;
import com.racetrack.repository.UserRepository;
import com.racetrack.repository.WorkoutLogRepository;
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

    @Test
    void submitWorkoutLog_setsSelectedDateAtStartOfDay() {
        User user = userRepository.save(user("runner-w1", "w1@example.com"));
        WorkoutLog log = workoutLog(user, "Workout", "8x400");

        WorkoutLog saved = workoutLogService.submitWorkoutLog(log, LocalDate.of(2026, 3, 1));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLogDate()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
    }

    @Test
    void findByUserId_returnsNewestFirst() {
        User user = userRepository.save(user("runner-w2", "w2@example.com"));
        WorkoutLog older = workoutLog(user, "Strength", "Core");
        older.setLogDate(LocalDateTime.of(2026, 2, 1, 0, 0));
        WorkoutLog newer = workoutLog(user, "Workout", "Tempo");
        newer.setLogDate(LocalDateTime.of(2026, 3, 1, 0, 0));
        workoutLogRepository.save(older);
        workoutLogRepository.save(newer);

        List<WorkoutLog> logs = workoutLogService.findByUserId("runner-w2");

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).getCompletionDetails()).isEqualTo("Tempo");
        assertThat(logs.get(1).getCompletionDetails()).isEqualTo("Core");
    }

    @Test
    void updateAthleteOwnedLog_updatesFieldsWhenOwnedByUser() {
        User user = userRepository.save(user("runner-w3", "w3@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(user, "Strength", "Before"));

        WorkoutLog updated = workoutLogService.updateAthleteOwnedLog(
                "runner-w3",
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
    void updateAthleteOwnedLog_throwsNotFoundWhenNotOwned() {
        assertThatThrownBy(() -> workoutLogService.updateAthleteOwnedLog(
                "missing",
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
    void deleteAthleteOwnedLog_removesRow() {
        User user = userRepository.save(user("runner-w4", "w4@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(user, "Strength", "Delete me"));

        workoutLogService.deleteAthleteOwnedLog("runner-w4", log.getId());

        assertThat(workoutLogRepository.findById(log.getId())).isEmpty();
    }

    @Test
    void updateCoachComment_trimsAndNullsBlank() {
        User user = userRepository.save(user("runner-w5", "w5@example.com"));
        WorkoutLog log = workoutLogRepository.save(workoutLog(user, "Workout", "Comment target"));

        WorkoutLog trimmed = workoutLogService.updateCoachComment(log.getId(), "  Strong day  ");
        assertThat(trimmed.getCoachComment()).isEqualTo("Strong day");

        WorkoutLog blank = workoutLogService.updateCoachComment(log.getId(), "   ");
        assertThat(blank.getCoachComment()).isNull();
    }

    private User user(String id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole("athlete");
        return user;
    }

    private WorkoutLog workoutLog(User user, String type, String completionDetails) {
        WorkoutLog log = new WorkoutLog();
        log.setUser(user);
        log.setWorkoutType(type);
        log.setCompletionDetails(completionDetails);
        return log;
    }
}
