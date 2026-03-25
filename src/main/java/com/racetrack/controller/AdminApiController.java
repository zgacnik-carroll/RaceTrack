package com.racetrack.controller;

import com.racetrack.model.User;
import com.racetrack.service.AdminService;
import com.racetrack.service.UserService;
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

    private final AdminService adminService;
    private final UserService userService;

    public AdminApiController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    /**
     * Creates a new athlete in Okta and seeds the local user table.
     *
     * @param request athlete creation payload
     * @param oidcUser authenticated coach
     * @return created athlete summary
     */
    @PostMapping("/athletes")
    public CreatedAthleteResponse createAthlete(@RequestBody CreateAthleteRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser) {
        requireCoach(oidcUser);
        User athlete = adminService.createAthlete(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.temporaryPassword()
        );
        return new CreatedAthleteResponse(
                athlete.getId(),
                athlete.getEmail(),
                athlete.getFullName(),
                athlete.getRole()
        );
    }

    /**
     * Updates an athlete in Okta and in the application.
     *
     * @param athleteId target athlete id
     * @param request athlete update payload
     * @param oidcUser authenticated coach
     * @return updated athlete summary
     */
    @PutMapping("/athletes/{athleteId}")
    public CreatedAthleteResponse updateAthlete(@PathVariable String athleteId,
                                                @RequestBody UpdateAthleteRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser) {
        requireCoach(oidcUser);
        User athlete = adminService.updateAthlete(
                athleteId,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.temporaryPassword()
        );
        return new CreatedAthleteResponse(
                athlete.getId(),
                athlete.getEmail(),
                athlete.getFullName(),
                athlete.getRole()
        );
    }

    /**
     * Deletes an athlete from Okta and from the application.
     *
     * @param athleteId target athlete id
     * @param oidcUser authenticated coach
     * @return empty success response
     */
    @DeleteMapping("/athletes/{athleteId}")
    public ResponseEntity<Void> deleteAthlete(@PathVariable String athleteId,
                                              @AuthenticationPrincipal OidcUser oidcUser) {
        requireCoach(oidcUser);
        adminService.deleteAthlete(athleteId);
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
        requireCoach(oidcUser);
        adminService.clearAllLogData();
        return ResponseEntity.noContent().build();
    }

    private void requireCoach(OidcUser oidcUser) {
        User currentUser = userService.getOrCreateForApi(oidcUser);
        if (!userService.isCoach(currentUser)) {
            throw new ResponseStatusException(FORBIDDEN, "Only coaches can access admin actions.");
        }
    }

    public record CreateAthleteRequest(
            String firstName,
            String lastName,
            String email,
            String temporaryPassword
    ) {}

    public record UpdateAthleteRequest(
            String firstName,
            String lastName,
            String email,
            String temporaryPassword
    ) {}

    public record CreatedAthleteResponse(
            String id,
            String email,
            String fullName,
            String role
    ) {}
}
