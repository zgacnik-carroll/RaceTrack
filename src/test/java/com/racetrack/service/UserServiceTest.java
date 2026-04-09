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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getAuthorizedUserForHome_updatesExistingUserFieldsAndDefaultRole() {
        User existing = user("user-1", "new@example.com", "Old Name", "");
        userRepository.save(existing);

        User result = userService.getAuthorizedUserForHome(oidc("ignored", "new@example.com", "New Name"));

        assertThat(result.getId()).isEqualTo("user-1");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFullName()).isEqualTo("New Name");
        assertThat(result.getRole()).isEqualTo("athlete");
    }

    @Test
    void getAuthorizedUserForHome_rejectsEmailNotInDatabase() {
        assertThatThrownBy(() -> userService.getAuthorizedUserForHome(oidc("ignored", "newuser@example.com", "New User")))
                .hasMessageContaining("Please reach out to your coach");
    }

    @Test
    void getAuthorizedUserForHome_doesNotOverwriteFullNameWithBlank() {
        userRepository.save(user("user-1b", "user1b@example.com", "Existing Name", "athlete"));

        User result = userService.getAuthorizedUserForHome(oidc("ignored", "user1b@example.com", "  "));

        assertThat(result.getFullName()).isEqualTo("Existing Name");
    }

    @Test
    void getAuthorizedUserForApi_onlyFillsMissingFields() {
        userRepository.save(user("user-2", "keep@example.com", "Keep Name", "athlete"));

        User result = userService.getAuthorizedUserForApi(oidc("ignored", "keep@example.com", "New Name"));

        assertThat(result.getEmail()).isEqualTo("keep@example.com");
        assertThat(result.getFullName()).isEqualTo("Keep Name");
        assertThat(result.getRole()).isEqualTo("athlete");
    }

    @Test
    void getAuthorizedUserForApi_rejectsUnknownEmail() {
        assertThatThrownBy(() -> userService.getAuthorizedUserForApi(oidc("ignored", "apinew@example.com", "API User")))
                .hasMessageContaining("Please reach out to your coach");
    }

    @Test
    void getAuthorizedUserForApi_normalizesStoredEmailCase() {
        userRepository.save(user("user-2b", "Filled@Example.com", "Has Name", "athlete"));

        User result = userService.getAuthorizedUserForApi(oidc("ignored", "filled@example.com", "Has Name"));

        assertThat(result.getEmail()).isEqualTo("filled@example.com");
    }

    @Test
    void getAuthorizedUserForApi_doesNotFillFullNameFromApiLogin() {
        userRepository.save(user("user-2c", "user2c@example.com", null, "athlete"));

        User result = userService.getAuthorizedUserForApi(oidc("ignored", "user2c@example.com", "Filled Name"));

        assertThat(result.getFullName()).isNull();
    }

    @Test
    void getAuthorizedUserForFormSubmit_returnsExistingUserByEmail() {
        userRepository.save(user("user-3", "form@example.com", "Form User", "athlete"));

        User result = userService.getAuthorizedUserForFormSubmit(oidc("ignored", "form@example.com", "Form User"));

        assertThat(result.getId()).isEqualTo("user-3");
        assertThat(result.getEmail()).isEqualTo("form@example.com");
    }

    @Test
    void getAuthorizedUserForFormSubmit_rejectsUnknownEmail() {
        assertThatThrownBy(() -> userService.getAuthorizedUserForFormSubmit(oidc("ignored", "missing@example.com", "Form User")))
                .hasMessageContaining("Please reach out to your coach");
    }

    @Test
    void isAuthorizedEmail_returnsTrueOnlyWhenEmailExists() {
        userRepository.save(user("user-3b", "existing@example.com", "Existing", "coach"));

        assertThat(userService.isAuthorizedEmail(oidc("ignored", "existing@example.com", "Existing"))).isTrue();
        assertThat(userService.isAuthorizedEmail(oidc("ignored", "missing@example.com", "Missing"))).isFalse();
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
    void displayNameAndIsCoachBehaveAsExpected() {
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
