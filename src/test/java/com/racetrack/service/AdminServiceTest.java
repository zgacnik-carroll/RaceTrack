package com.racetrack.service;

import com.racetrack.model.User;
import com.racetrack.repository.RunningLogRepository;
import com.racetrack.repository.UserRepository;
import com.racetrack.repository.WorkoutLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private OktaAdminClient oktaAdminClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RunningLogRepository runningLogRepository;

    @Mock
    private WorkoutLogRepository workoutLogRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void createAthlete_createsOktaUserAndLocalAthleteRecord() {
        when(oktaAdminClient.createAthlete("Jane", "Doe", "jane@example.com", "TempPass123"))
                .thenReturn(new OktaAdminClient.CreatedOktaUser("okta-123", "jane@example.com"));
        when(userRepository.findById("okta-123")).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User created = adminService.createAthlete(" Jane ", " Doe ", " JANE@example.com ", " TempPass123 ");

        assertThat(created.getId()).isEqualTo("okta-123");
        assertThat(created.getEmail()).isEqualTo("jane@example.com");
        assertThat(created.getFullName()).isEqualTo("Jane Doe");
        assertThat(created.getRole()).isEqualTo("athlete");
    }

    @Test
    void createAthlete_rejectsBlankFirstNameBeforeCallingOkta() {
        assertThatThrownBy(() -> adminService.createAthlete(" ", "Doe", "jane@example.com", null))
                .hasMessageContaining("First name is required.");

        verify(oktaAdminClient, never()).createAthlete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void clearAllLogData_deletesOnlyLogs() {
        adminService.clearAllLogData();

        verify(workoutLogRepository).deleteAllInBatch();
        verify(runningLogRepository).deleteAllInBatch();
        verify(userRepository, never()).delete(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void deleteAthlete_deletesOktaUserLogsAndLocalUser() {
        User athlete = new User();
        athlete.setId("ath-1");
        athlete.setRole("athlete");
        when(userRepository.findById("ath-1")).thenReturn(Optional.of(athlete));

        adminService.deleteAthlete("ath-1");

        verify(oktaAdminClient).deleteUser("ath-1");
        verify(workoutLogRepository).deleteByUser_Id("ath-1");
        verify(runningLogRepository).deleteByUser_Id("ath-1");
        verify(userRepository).delete(athlete);
    }

    @Test
    void deleteAthlete_rejectsDeletingNonAthlete() {
        User coach = new User();
        coach.setId("coach-1");
        coach.setRole("coach");
        when(userRepository.findById("coach-1")).thenReturn(Optional.of(coach));

        assertThatThrownBy(() -> adminService.deleteAthlete("coach-1"))
                .hasMessageContaining("Only athletes can be deleted.");

        verify(oktaAdminClient, never()).deleteUser("coach-1");
    }
}
