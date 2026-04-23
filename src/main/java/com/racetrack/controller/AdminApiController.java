package com.racetrack.controller;

import com.racetrack.model.User;
import com.racetrack.service.AdminService;
import com.racetrack.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Coach-only admin endpoints for roster management and destructive maintenance actions.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private static final Logger log = LoggerFactory.getLogger(AdminApiController.class);

    private final AdminService adminService;
    private final UserService userService;

    /**
     * Creates the coach-only admin API controller.
     *
     * @param adminService service for local user-management actions
     * @param userService service for resolving and authorizing the current user
     */
    public AdminApiController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    /**
     * Creates a new user in the application database.
     *
     * @param request user creation payload
     * @param oidcUser authenticated coach
     * @return created user summary
     */
    @PostMapping("/users")
    public CreatedUserResponse createUser(@RequestBody CreateUserRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = requireCoach(oidcUser);
        log.info("Admin create user requested by coachUserId={} email={} role={}",
                currentUser.getId(), request.email(), request.role());
        User user = adminService.createUser(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.role()
        );
        return new CreatedUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }

    /**
     * Updates a user in the application database.
     *
     * @param athleteId target athlete id
     * @param request athlete update payload
     * @param oidcUser authenticated coach
     * @return updated athlete summary
     */
    @PutMapping("/users/{userId}")
    public CreatedUserResponse updateUser(@PathVariable String userId,
                                          @RequestBody UpdateUserRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = requireCoach(oidcUser);
        log.info("Admin update user requested by coachUserId={} targetUserId={} email={} role={}",
                currentUser.getId(), userId, request.email(), request.role());
        User user = adminService.updateUser(
                userId,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.role()
        );
        return new CreatedUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }

    /**
     * Deletes a user from the application database.
     *
     * @param athleteId target athlete id
     * @param oidcUser authenticated coach
     * @return empty success response
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId,
                                           @AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = requireCoach(oidcUser);
        log.info("Admin delete user requested by coachUserId={} targetUserId={}", currentUser.getId(), userId);
        adminService.deleteUser(userId);
        log.info("Admin delete user completed by coachUserId={} targetUserId={}", currentUser.getId(), userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clears persisted log data while preserving all users.
     *
     * @param oidcUser authenticated coach
     * @return empty success response
     */
    @DeleteMapping("/data")
    public ResponseEntity<Void> clearData(@AuthenticationPrincipal OidcUser oidcUser) {
        User currentUser = requireCoach(oidcUser);
        log.info("Admin clear data requested by coachUserId={}", currentUser.getId());
        adminService.clearAllLogData();
        log.info("Admin clear data completed by coachUserId={}", currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves the current user and rejects the request unless that user has
     * the coach role.
     *
     * @param oidcUser authenticated principal from Spring Security
     * @return resolved coach user
     */
    private User requireCoach(OidcUser oidcUser) {
        User currentUser = userService.getAuthorizedUserForApi(oidcUser);
        if (!userService.isCoach(currentUser)) {
            log.warn("Forbidden admin action attempt by non-coach userId={}", currentUser.getId());
            throw new ResponseStatusException(FORBIDDEN, "Only coaches can access admin actions.");
        }
        return currentUser;
    }

    /**
     * Request payload for creating a local RaceTrack user.
     *
     * @param firstName user first name
     * @param lastName user last name
     * @param email login email that must match Okta authentication
     * @param role application role to assign
     */
    public record CreateUserRequest(
            String firstName,
            String lastName,
            String email,
            String role
    ) {}

    /**
     * Request payload for updating a local RaceTrack user.
     *
     * @param firstName updated first name
     * @param lastName updated last name
     * @param email updated login email
     * @param role updated application role
     */
    public record UpdateUserRequest(
            String firstName,
            String lastName,
            String email,
            String role
    ) {}

    /**
     * Response payload returned after a user create/update operation.
     *
     * @param id local RaceTrack user id
     * @param email user email
     * @param fullName display name shown in the UI
     * @param role assigned application role
     */
    public record CreatedUserResponse(
            String id,
            String email,
            String fullName,
            String role
    ) {}
}
