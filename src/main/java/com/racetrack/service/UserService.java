package com.racetrack.service;

import com.racetrack.model.User;
import com.racetrack.repository.UserRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * User operations grouped by model-level behavior.
 */
@Service
public class UserService {

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
     * Loads or creates a user for the home page flow.
     *
     * @param oidcUser authenticated identity payload
     * @return existing or newly created user
     */
    public User getOrCreateForHome(OidcUser oidcUser) {
        String oktaId = oidcUser.getSubject();
        return userRepository.findById(oktaId)
                .map(existing -> {
                    existing.setEmail(oidcUser.getEmail());
                    if (oidcUser.getFullName() != null && !oidcUser.getFullName().isBlank()) {
                        existing.setFullName(oidcUser.getFullName());
                    }
                    if (existing.getRole() == null || existing.getRole().isBlank()) {
                        existing.setRole("athlete");
                    }
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(oktaId);
                    newUser.setEmail(oidcUser.getEmail());
                    newUser.setFullName(oidcUser.getFullName());
                    newUser.setRole("athlete");
                    return userRepository.save(newUser);
                });
    }

    /**
     * Loads or creates a user for API calls.
     *
     * @param oidcUser authenticated identity payload
     * @return existing or newly created user
     */
    public User getOrCreateForApi(OidcUser oidcUser) {
        String oktaId = oidcUser.getSubject();
        return userRepository.findById(oktaId)
                .map(existing -> {
                    if (existing.getEmail() == null || existing.getEmail().isBlank()) {
                        existing.setEmail(oidcUser.getEmail());
                    }
                    String name = oidcUser.getFullName();
                    if (name != null && !name.isBlank() &&
                            (existing.getFullName() == null || existing.getFullName().isBlank())) {
                        existing.setFullName(name);
                    }
                    if (existing.getRole() == null || existing.getRole().isBlank()) {
                        existing.setRole("athlete");
                    }
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(oktaId);
                    newUser.setEmail(oidcUser.getEmail());
                    newUser.setFullName(oidcUser.getFullName());
                    newUser.setRole("athlete");
                    return userRepository.save(newUser);
                });
    }

    /**
     * Loads or creates a user for form submissions.
     *
     * @param oidcUser authenticated identity payload
     * @return existing or newly created user
     */
    public User getOrCreateForFormSubmit(OidcUser oidcUser) {
        String oktaId = oidcUser.getSubject();
        return userRepository.findById(oktaId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(oktaId);
                    newUser.setEmail(oidcUser.getEmail());
                    newUser.setRole("athlete");
                    return userRepository.save(newUser);
                });
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
}
