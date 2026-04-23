package com.racetrack.service;

import com.racetrack.model.User;
import com.racetrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * User operations grouped by model-level behavior.
 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    /**
     * Creates a user service.
     *
     * @param userRepository persistent store for users
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads an authorized user for the home page flow.
     *
     * @param oidcUser authenticated identity payload
     * @return existing user matched by email
     */
    public User getAuthorizedUserForHome(OidcUser oidcUser) {
        return resolveAuthorizedUser(oidcUser, true);
    }

    /**
     * Loads an authorized user for API calls.
     *
     * @param oidcUser authenticated identity payload
     * @return existing user matched by email
     */
    public User getAuthorizedUserForApi(OidcUser oidcUser) {
        return resolveAuthorizedUser(oidcUser, false);
    }

    /**
     * Loads an authorized user for form submissions.
     *
     * @param oidcUser authenticated identity payload
     * @return existing user matched by email
     */
    public User getAuthorizedUserForFormSubmit(OidcUser oidcUser) {
        return resolveAuthorizedUser(oidcUser, false);
    }

    /**
     * Checks whether a login email belongs to a user in the local database.
     *
     * @param oidcUser authenticated identity payload
     * @return true when the email is authorized for app access
     */
    public boolean isAuthorizedEmail(OidcUser oidcUser) {
        String normalizedEmail = normalizeEmail(oidcUser);
        return userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent();
    }

    /**
     * Returns all athletes sorted by full name.
     *
     * @return ordered athlete list
     */
    public List<User> getAthletesOrderedByName() {
        return userRepository.findByRoleIgnoreCaseOrderByFullNameAsc("athlete");
    }

    /**
     * Returns all users sorted by full name.
     *
     * @return ordered user list
     */
    public List<User> getUsersOrderedByName() {
        return userRepository.findAllByOrderByFullNameAsc();
    }

    /**
     * Checks whether the given user has coach role.
     *
     * @param user user to inspect
     * @return true when role is coach
     */
    public boolean isCoach(User user) {
        return user.getRole() != null && "coach".equalsIgnoreCase(user.getRole());
    }

    /**
     * Computes display name used in the UI header.
     *
     * @param user user to display
     * @return full name, email, or fallback label
     */
    public String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return "User";
    }

    /**
     * Resolves the current user by email and optionally refreshes local profile
     * metadata from the latest OIDC payload.
     *
     * @param oidcUser authenticated identity payload
     * @param updateNameFromLogin whether the local full name should be refreshed from Okta
     * @return authorized local user
     */
    private User resolveAuthorizedUser(OidcUser oidcUser, boolean updateNameFromLogin) {
        String normalizedEmail = normalizeEmail(oidcUser);
        log.info("Authorizing user email={} updateNameFromLogin={}", normalizedEmail, updateNameFromLogin);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Your email does not have access to RaceTrack. Please reach out to your coach."
                ));

        boolean changed = false;
        if (user.getEmail() == null || !normalizedEmail.equals(user.getEmail())) {
            user.setEmail(normalizedEmail);
            changed = true;
        }
        // Backfill a default role for any older rows that predate strict role assignment.
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("athlete");
            changed = true;
        }
        String fullName = oidcUser.getFullName();
        if (updateNameFromLogin && fullName != null && !fullName.isBlank() && !fullName.equals(user.getFullName())) {
            user.setFullName(fullName);
            changed = true;
        }

        if (changed) {
            log.info("Authorized user metadata updated userId={} email={}", user.getId(), normalizedEmail);
        }
        return changed ? userRepository.save(user) : user;
    }

    /**
     * Normalizes the email claim from the OIDC user and rejects logins that do
     * not provide one.
     *
     * @param oidcUser authenticated identity payload
     * @return trimmed, lowercased email
     */
    private String normalizeEmail(OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Authorization failed because login email was missing");
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your login did not provide an email address. Please reach out to your coach."
            );
        }
        return email.trim().toLowerCase();
    }
}
