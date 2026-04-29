package edu.carroll.racetrack.service;

import edu.carroll.racetrack.model.RunningLog;
import edu.carroll.racetrack.model.User;
import edu.carroll.racetrack.model.WorkoutLog;
import edu.carroll.racetrack.repository.RunningLogRepository;
import edu.carroll.racetrack.repository.UserRepository;
import edu.carroll.racetrack.repository.WorkoutLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AdminService} backed by in-memory H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RunningLogRepository runningLogRepository;

    @Autowired
    private WorkoutLogRepository workoutLogRepository;

    @Test
    void createUser_createsLocalUserRecord() {
        User created = adminService.createUser(" Jane ", " Doe ", " JANE@example.com ", " athlete ");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("jane@example.com");
        assertThat(created.getFullName()).isEqualTo("Jane Doe");
        assertThat(created.getRole()).isEqualTo("athlete");
        assertThat(userRepository.findById(created.getId())).contains(created);
    }

    @Test
    void createUser_rejectsBlankFirstNameBeforeSaving() {
        assertThatThrownBy(() -> adminService.createUser(" ", "Doe", "jane@example.com", "athlete"))
                .hasMessageContaining("First name is required.");

        assertThat(userRepository.findAll()).isEmpty();
    }

    @Test
    void createUser_rejectsDuplicateEmail() {
        userRepository.save(user("jane@example.com", "Existing User", "athlete"));

        assertThatThrownBy(() -> adminService.createUser("Jane", "Doe", "jane@example.com", "athlete"))
                .hasMessageContaining("A user with that email already exists.");
    }

    @Test
    void updateUser_updatesLocalUserRecord() {
        User athlete = userRepository.save(user("old@example.com", "Old Name", "athlete"));

        User updated = adminService.updateUser(athlete.getId(), "Jane", "Runner", "jane@example.com", "coach");

        assertThat(updated.getId()).isEqualTo(athlete.getId());
        assertThat(updated.getEmail()).isEqualTo("jane@example.com");
        assertThat(updated.getFullName()).isEqualTo("Jane Runner");
        assertThat(updated.getRole()).isEqualTo("coach");
    }

    @Test
    void updateUser_rejectsInvalidRole() {
        User coach = userRepository.save(user("coach@example.com", "Coach User", "coach"));

        assertThatThrownBy(() -> adminService.updateUser(coach.getId(), "A", "B", "coach@example.com", "manager"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Role must be athlete or coach.")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void clearAllLogData_deletesOnlyLogs() {
        User user = userRepository.save(user("athlete@example.com", "Athlete User", "athlete"));
        runningLogRepository.save(runningLog(user, "Run entry"));
        workoutLogRepository.save(workoutLog(user, "Workout entry"));

        adminService.clearAllLogData();

        assertThat(runningLogRepository.findAll()).isEmpty();
        assertThat(workoutLogRepository.findAll()).isEmpty();
        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void deleteUser_deletesLogsAndLocalUser() {
        User athlete = userRepository.save(user("athlete@example.com", "Athlete User", "athlete"));
        runningLogRepository.save(runningLog(athlete, "Run entry"));
        workoutLogRepository.save(workoutLog(athlete, "Workout entry"));

        adminService.deleteUser(athlete.getId());

        assertThat(userRepository.findById(athlete.getId())).isEmpty();
        assertThat(runningLogRepository.findAll()).isEmpty();
        assertThat(workoutLogRepository.findAll()).isEmpty();
    }

    @Test
    void deleteUser_deletesCoachToo() {
        User coach = userRepository.save(user("coach@example.com", "Coach User", "coach"));

        adminService.deleteUser(coach.getId());

        assertThat(userRepository.findById(coach.getId())).isEmpty();
    }

    private User user(String email, String fullName, String role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        return user;
    }

    private RunningLog runningLog(User user, String details) {
        RunningLog log = new RunningLog();
        log.setUser(user);
        log.setMileage(5.0);
        log.setHurting(false);
        log.setPainDetails(null);
        log.setSleepHours(8.0);
        log.setStressLevel(3);
        log.setPlateProportion(true);
        log.setGotThatBread(true);
        log.setFeel("Solid overall");
        log.setRpe(5);
        log.setDetails(details);
        log.setLogDate(LocalDateTime.of(2026, 3, 1, 0, 0));
        return log;
    }

    private WorkoutLog workoutLog(User user, String completionDetails) {
        WorkoutLog log = new WorkoutLog();
        log.setUser(user);
        log.setWorkoutType("Workout");
        log.setCompletionDetails(completionDetails);
        log.setActualPaces("Steady");
        log.setWorkoutDescription("Workout details");
        log.setLogDate(LocalDateTime.of(2026, 3, 1, 0, 0));
        return log;
    }
}
