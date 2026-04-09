package com.racetrack.service;

import com.racetrack.model.User;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.UserRepository;
import com.racetrack.repository.WorkoutLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RunningLogRepository runningLogRepository;

    @Mock
    private WorkoutLogRepository workoutLogRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, runningLogRepository, workoutLogRepository);
    }

    @Test
    void createUser_createsLocalUserRecord() {
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = adminService.createUser(" Jane ", " Doe ", " JANE@example.com ", " athlete ");

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getEmail()).isEqualTo("jane@example.com");
        assertThat(created.getFullName()).isEqualTo("Jane Doe");
        assertThat(created.getRole()).isEqualTo("athlete");
    }

    @Test
    void createUser_rejectsBlankFirstNameBeforeSaving() {
        assertThatThrownBy(() -> adminService.createUser(" ", "Doe", "jane@example.com", "athlete"))
                .hasMessageContaining("First name is required.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createUser("Jane", "Doe", "jane@example.com", "athlete"))
                .hasMessageContaining("A user with that email already exists.");
    }

    @Test
    void updateUser_updatesLocalUserRecord() {
        User athlete = new User();
        athlete.setId("ath-2");
        athlete.setRole("athlete");
        athlete.setEmail("old@example.com");
        athlete.setFullName("Old Name");
        when(userRepository.findById("ath-2")).thenReturn(Optional.of(athlete));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("jane@example.com", "ath-2")).thenReturn(false);
        when(userRepository.save(athlete)).thenReturn(athlete);

        User updated = adminService.updateUser("ath-2", "Jane", "Runner", "jane@example.com", "coach");

        assertThat(updated.getEmail()).isEqualTo("jane@example.com");
        assertThat(updated.getFullName()).isEqualTo("Jane Runner");
        assertThat(updated.getRole()).isEqualTo("coach");
    }

    @Test
    void updateUser_rejectsInvalidRole() {
        User coach = new User();
        coach.setId("coach-1");
        coach.setRole("coach");
        when(userRepository.findById("coach-1")).thenReturn(Optional.of(coach));

        assertThatThrownBy(() -> adminService.updateUser("coach-1", "A", "B", "coach@example.com", "manager"))
                .hasMessageContaining("Role must be athlete or coach.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void clearAllLogData_deletesOnlyLogs() {
        adminService.clearAllLogData();

        verify(workoutLogRepository).deleteAllInBatch();
        verify(runningLogRepository).deleteAllInBatch();
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_deletesLogsAndLocalUser() {
        User athlete = new User();
        athlete.setId("ath-1");
        athlete.setRole("athlete");
        when(userRepository.findById("ath-1")).thenReturn(Optional.of(athlete));

        adminService.deleteUser("ath-1");

        verify(workoutLogRepository).deleteByUser_Id("ath-1");
        verify(runningLogRepository).deleteByUser_Id("ath-1");
        verify(userRepository).delete(athlete);
    }

    @Test
    void deleteUser_deletesCoachToo() {
        User coach = new User();
        coach.setId("coach-1");
        coach.setRole("coach");
        when(userRepository.findById("coach-1")).thenReturn(Optional.of(coach));

        adminService.deleteUser("coach-1");

        verify(userRepository).delete(coach);
    }
}
