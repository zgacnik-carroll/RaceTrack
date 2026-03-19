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

    // -------------------------------------------------------------------------
    // getOrCreateForHome
    // -------------------------------------------------------------------------

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
    void getOrCreateForHome_createsNewUserWithAthleteRoleWhenNotFound() {
        User result = userService.getOrCreateForHome(oidc("new-user-1", "newuser@example.com", "New User"));

        assertThat(result.getId()).isEqualTo("new-user-1");
        assertThat(result.getEmail()).isEqualTo("newuser@example.com");
        assertThat(result.getFullName()).isEqualTo("New User");
        assertThat(result.getRole()).isEqualTo("athlete");
    }

    @Test
    void getOrCreateForHome_doesNotOverwriteExistingCoachRole() {
        User coach = new User();
        coach.setId("coach-home-1");
        coach.setEmail("coach@example.com");
        coach.setFullName("Coach Person");
        coach.setRole("coach");
        userRepository.save(coach);

        User result = userService.getOrCreateForHome(oidc("coach-home-1", "coach@example.com", "Coach Person"));

        assertThat(result.getRole()).isEqualTo("coach");
    }

    @Test
    void getOrCreateForHome_doesNotOverwriteFullNameWithBlank() {
        User existing = new User();
        existing.setId("user-1b");
        existing.setEmail("user1b@example.com");
        existing.setFullName("Existing Name");
        existing.setRole("athlete");
        userRepository.save(existing);

        User result = userService.getOrCreateForHome(oidc("user-1b", "user1b@example.com", "  "));

        assertThat(result.getFullName()).isEqualTo("Existing Name");
    }

    // -------------------------------------------------------------------------
    // getOrCreateForApi
    // -------------------------------------------------------------------------

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
    void getOrCreateForApi_createsNewUserWhenNotFound() {
        User result = userService.getOrCreateForApi(oidc("api-new-1", "apinew@example.com", "API User"));

        assertThat(result.getId()).isEqualTo("api-new-1");
        assertThat(result.getEmail()).isEqualTo("apinew@example.com");
        assertThat(result.getFullName()).isEqualTo("API User");
        assertThat(result.getRole()).isEqualTo("athlete");
    }

    @Test
    void getOrCreateForApi_fillsEmailWhenMissing() {
        User existing = new User();
        existing.setId("user-2b");
        existing.setEmail(null);
        existing.setFullName("Has Name");
        existing.setRole("athlete");
        userRepository.save(existing);

        User result = userService.getOrCreateForApi(oidc("user-2b", "filled@example.com", "Has Name"));

        assertThat(result.getEmail()).isEqualTo("filled@example.com");
    }

    @Test
    void getOrCreateForApi_fillsFullNameWhenMissing() {
        User existing = new User();
        existing.setId("user-2c");
        existing.setEmail("user2c@example.com");
        existing.setFullName(null);
        existing.setRole("athlete");
        userRepository.save(existing);

        User result = userService.getOrCreateForApi(oidc("user-2c", "user2c@example.com", "Filled Name"));

        assertThat(result.getFullName()).isEqualTo("Filled Name");
    }

    @Test
    void getOrCreateForApi_defaultsRoleToAthleteWhenBlank() {
        User existing = new User();
        existing.setId("user-2d");
        existing.setEmail("user2d@example.com");
        existing.setFullName("No Role");
        existing.setRole(null);
        userRepository.save(existing);

        User result = userService.getOrCreateForApi(oidc("user-2d", "user2d@example.com", "No Role"));

        assertThat(result.getRole()).isEqualTo("athlete");
    }

    // -------------------------------------------------------------------------
    // getOrCreateForFormSubmit
    // -------------------------------------------------------------------------

    @Test
    void getOrCreateForFormSubmit_createsAthleteWhenMissing() {
        User result = userService.getOrCreateForFormSubmit(oidc("user-3", "form@example.com", "Form User"));

        assertThat(result.getId()).isEqualTo("user-3");
        assertThat(result.getEmail()).isEqualTo("form@example.com");
        assertThat(result.getRole()).isEqualTo("athlete");
        assertThat(result.getFullName()).isNull();
    }

    @Test
    void getOrCreateForFormSubmit_returnsExistingUserWithoutModifying() {
        User existing = new User();
        existing.setId("user-3b");
        existing.setEmail("existing@example.com");
        existing.setFullName("Existing");
        existing.setRole("coach");
        userRepository.save(existing);

        User result = userService.getOrCreateForFormSubmit(oidc("user-3b", "new@example.com", "New Name"));

        // getOrCreateForFormSubmit only creates; it does not update existing records
        assertThat(result.getId()).isEqualTo("user-3b");
        assertThat(result.getRole()).isEqualTo("coach");
    }

    // -------------------------------------------------------------------------
    // getAthletesOrderedByName
    // -------------------------------------------------------------------------

    @Test
    void getAthletesOrderedByName_returnsOnlyAthletesSortedByName() {
        userRepository.save(user("a1", "c@example.com", "Charlie", "athlete"));
        userRepository.save(user("a2", "a@example.com", "Alice", "athlete"));
        userRepository.save(user("c1", "coach@example.com", "Coach", "coach"));

        List<User> athletes = userService.getAthletesOrderedByName();

        assertThat(athletes).extracting(User::getFullName).containsExactly("Alice", "Charlie");
    }

    @Test
    void getAthletesOrderedByName_returnsEmptyListWhenNoAthletes() {
        userRepository.save(user("c2", "onlycoach@example.com", "Only Coach", "coach"));

        List<User> athletes = userService.getAthletesOrderedByName();

        assertThat(athletes).isEmpty();
    }

    @Test
    void getAthletesOrderedByName_isCaseInsensitiveForRoleMatch() {
        userRepository.save(user("a3", "upper@example.com", "Upper Athlete", "ATHLETE"));

        List<User> athletes = userService.getAthletesOrderedByName();

        assertThat(athletes).extracting(User::getFullName).contains("Upper Athlete");
    }

    // -------------------------------------------------------------------------
    // isCoach
    // -------------------------------------------------------------------------

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

    @Test
    void isCoach_returnsFalseForAthleteRole() {
        User athlete = user("ath-3", "ath3@example.com", "Athlete", "athlete");

        assertThat(userService.isCoach(athlete)).isFalse();
    }

    @Test
    void isCoach_returnsFalseForNullRole() {
        User noRole = user("ath-4", "ath4@example.com", "No Role", null);

        assertThat(userService.isCoach(noRole)).isFalse();
    }

    @Test
    void isCoach_isCaseInsensitive() {
        User upperCoach = user("coach-2", "uppercoach@example.com", "Upper Coach", "COACH");

        assertThat(userService.isCoach(upperCoach)).isTrue();
    }

    // -------------------------------------------------------------------------
    // displayName
    // -------------------------------------------------------------------------

    @Test
    void displayName_prefersFullNameOverEmail() {
        User user = user("dn-1", "email@example.com", "Full Name", "athlete");

        assertThat(userService.displayName(user)).isEqualTo("Full Name");
    }

    @Test
    void displayName_usesEmailWhenFullNameIsBlank() {
        User user = user("dn-2", "email@example.com", "  ", "athlete");

        assertThat(userService.displayName(user)).isEqualTo("email@example.com");
    }

    @Test
    void displayName_returnsFallbackWhenBothNameAndEmailAreNull() {
        User user = user("dn-3", null, null, "athlete");

        assertThat(userService.displayName(user)).isEqualTo("User");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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