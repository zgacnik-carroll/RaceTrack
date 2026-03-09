package com.racetrack.service;

import com.racetrack.model.User;
import com.racetrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService} backed by in-memory H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getOrCreateForHome_updatesExistingUserFieldsAndDefaultRole() {
        User existing = new User();
        existing.setId("user-1");
        existing.setEmail("old@example.com");
        existing.setFullName("Old Name");
        existing.setRole("");
        userRepository.save(existing);

        User result = userService.getOrCreateForHome(oidc("user-1", "new@example.com", "New Name"));

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFullName()).isEqualTo("New Name");
        assertThat(result.getRole()).isEqualTo("athlete");
    }

    @Test
    void getOrCreateForApi_onlyFillsMissingFields() {
        User existing = new User();
        existing.setId("user-2");
        existing.setEmail("keep@example.com");
        existing.setFullName("Keep Name");
        existing.setRole("athlete");
        userRepository.save(existing);

        User result = userService.getOrCreateForApi(oidc("user-2", "new@example.com", "New Name"));

        assertThat(result.getEmail()).isEqualTo("keep@example.com");
        assertThat(result.getFullName()).isEqualTo("Keep Name");
        assertThat(result.getRole()).isEqualTo("athlete");
    }

    @Test
    void getOrCreateForFormSubmit_createsAthleteWhenMissing() {
        User result = userService.getOrCreateForFormSubmit(oidc("user-3", "form@example.com", "Form User"));

        assertThat(result.getId()).isEqualTo("user-3");
        assertThat(result.getEmail()).isEqualTo("form@example.com");
        assertThat(result.getRole()).isEqualTo("athlete");
        assertThat(result.getFullName()).isNull();
    }

    @Test
    void getAthletesOrderedByName_returnsOnlyAthletesSortedByName() {
        userRepository.save(user("a1", "c@example.com", "Charlie", "athlete"));
        userRepository.save(user("a2", "a@example.com", "Alice", "athlete"));
        userRepository.save(user("c1", "coach@example.com", "Coach", "coach"));

        List<User> athletes = userService.getAthletesOrderedByName();

        assertThat(athletes).extracting(User::getFullName).containsExactly("Alice", "Charlie");
    }

    @Test
    void displayName_and_isCoach_behaveAsExpected() {
        User coach = user("coach-1", "coach@example.com", "Coach Name", "coach");
        User emailOnly = user("ath-1", "athlete@example.com", null, "athlete");
        User fallback = user("ath-2", null, null, "athlete");

        assertThat(userService.isCoach(coach)).isTrue();
        assertThat(userService.displayName(coach)).isEqualTo("Coach Name");
        assertThat(userService.displayName(emailOnly)).isEqualTo("athlete@example.com");
        assertThat(userService.displayName(fallback)).isEqualTo("User");
    }

    private User user(String id, String email, String fullName, String role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        return user;
    }

    private OidcUser oidc(String subject, String email, String fullName) {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getSubject()).thenReturn(subject);
        when(oidcUser.getEmail()).thenReturn(email);
        when(oidcUser.getFullName()).thenReturn(fullName);
        return oidcUser;
    }
}
