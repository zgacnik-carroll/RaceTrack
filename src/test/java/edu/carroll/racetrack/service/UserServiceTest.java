package edu.carroll.racetrack.service;

import edu.carroll.racetrack.model.User;
import edu.carroll.racetrack.repository.UserRepository;
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
        User existing = userRepository.save(user("new@example.com", "Old Name", ""));

        User result = userService.getAuthorizedUserForHome(oidc("ignored", "new@example.com", "New Name"));

        assertThat(result.getId()).isEqualTo(existing.getId());
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
        userRepository.save(user("user1b@example.com", "Existing Name", "athlete"));

        User result = userService.getAuthorizedUserForHome(oidc("ignored", "user1b@example.com", "  "));

        assertThat(result.getFullName()).isEqualTo("Existing Name");
    }

    @Test
    void getAuthorizedUserForApi_onlyFillsMissingFields() {
        userRepository.save(user("keep@example.com", "Keep Name", "athlete"));

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
        userRepository.save(user("Filled@Example.com", "Has Name", "athlete"));

        User result = userService.getAuthorizedUserForApi(oidc("ignored", "filled@example.com", "Has Name"));

        assertThat(result.getEmail()).isEqualTo("filled@example.com");
    }

    @Test
    void getAuthorizedUserForApi_doesNotFillFullNameFromApiLogin() {
        userRepository.save(user("user2c@example.com", null, "athlete"));

        User result = userService.getAuthorizedUserForApi(oidc("ignored", "user2c@example.com", "Filled Name"));

        assertThat(result.getFullName()).isNull();
    }

    @Test
    void getAuthorizedUserForFormSubmit_returnsExistingUserByEmail() {
        User existing = userRepository.save(user("form@example.com", "Form User", "athlete"));

        User result = userService.getAuthorizedUserForFormSubmit(oidc("ignored", "form@example.com", "Form User"));

        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getEmail()).isEqualTo("form@example.com");
    }

    @Test
    void getAuthorizedUserForFormSubmit_rejectsUnknownEmail() {
        assertThatThrownBy(() -> userService.getAuthorizedUserForFormSubmit(oidc("ignored", "missing@example.com", "Form User")))
                .hasMessageContaining("Please reach out to your coach");
    }

    @Test
    void isAuthorizedEmail_returnsTrueOnlyWhenEmailExists() {
        userRepository.save(user("existing@example.com", "Existing", "coach"));

        assertThat(userService.isAuthorizedEmail(oidc("ignored", "existing@example.com", "Existing"))).isTrue();
        assertThat(userService.isAuthorizedEmail(oidc("ignored", "missing@example.com", "Missing"))).isFalse();
    }

    @Test
    void getAthletesOrderedByName_returnsOnlyAthletesSortedByName() {
        userRepository.save(user("c@example.com", "Charlie", "athlete"));
        userRepository.save(user("a@example.com", "Alice", "athlete"));
        userRepository.save(user("coach@example.com", "Coach", "coach"));

        List<User> athletes = userService.getAthletesOrderedByName();

        assertThat(athletes).extracting(User::getFullName).containsExactly("Alice", "Charlie");
    }

    @Test
    void displayNameAndIsCoachBehaveAsExpected() {
        User coach = user("coach@example.com", "Coach Name", "coach");
        User emailOnly = user("athlete@example.com", null, "athlete");
        User fallback = user(null, null, "athlete");

        assertThat(userService.isCoach(coach)).isTrue();
        assertThat(userService.displayName(coach)).isEqualTo("Coach Name");
        assertThat(userService.displayName(emailOnly)).isEqualTo("athlete@example.com");
        assertThat(userService.displayName(fallback)).isEqualTo("User");
    }

    private User user(String email, String fullName, String role) {
        User user = new User();
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

