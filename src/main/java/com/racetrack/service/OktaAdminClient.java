package com.racetrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal Okta management client for creating athlete accounts.
 */
@Component
public class OktaAdminClient {

    private final String orgUrl;
    private final String apiToken;

    public OktaAdminClient(@Value("${okta.management.org-url:}") String orgUrl,
                           @Value("${okta.management.api-token:}") String apiToken) {
        this.orgUrl = orgUrl;
        this.apiToken = apiToken;
    }

    /**
     * Creates a new Okta user with an optional temporary password.
     *
     * @param firstName first name
     * @param lastName last name
     * @param email email and login
     * @param temporaryPassword optional temporary password
     * @return Okta id and email for the created user
     */
    @SuppressWarnings("unchecked")
    public CreatedOktaUser createAthlete(String firstName,
                                         String lastName,
                                         String email,
                                         String temporaryPassword) {
        validateConfiguration();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("firstName", firstName);
        profile.put("lastName", lastName);
        profile.put("email", email);
        profile.put("login", email);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("profile", profile);

        if (temporaryPassword != null) {
            payload.put("credentials", Map.of("password", Map.of("value", temporaryPassword)));
        }

        try {
            Map<String, Object> response = restClient().post()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/users").queryParam("activate", true).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("id") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Okta did not return a created user id.");
            }

            Object profileNode = response.get("profile");
            String createdEmail = email;
            if (profileNode instanceof Map<?, ?> profileMap && profileMap.get("email") instanceof String profileEmail) {
                createdEmail = profileEmail;
            }

            return new CreatedOktaUser(String.valueOf(response.get("id")), createdEmail);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An Okta user with that email already exists.", ex);
            }
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Okta management credentials were rejected.", ex);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Okta user creation failed.", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach Okta management API.", ex);
        }
    }

    /**
     * Updates an Okta user's profile and optional password with partial-update semantics.
     *
     * @param userId Okta user id
     * @param firstName first name
     * @param lastName last name
     * @param email primary email / login
     * @param temporaryPassword optional password replacement
     */
    public void updateAthlete(String userId,
                              String firstName,
                              String lastName,
                              String email,
                              String temporaryPassword) {
        validateConfiguration();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("firstName", firstName);
        profile.put("lastName", lastName);
        profile.put("email", email);
        profile.put("login", email);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("profile", profile);

        if (temporaryPassword != null) {
            payload.put("credentials", Map.of("password", Map.of("value", temporaryPassword)));
        }

        try {
            restClient().post()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/users/{id}").build(userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An Okta user with that email already exists.", ex);
            }
            handleOktaError("Okta user update failed.", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach Okta management API.", ex);
        }
    }

    /**
     * Permanently deletes an Okta user after deactivation.
     *
     * @param userId Okta user id
     */
    public void deleteUser(String userId) {
        validateConfiguration();

        try {
            restClient().post()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/users/{id}/lifecycle/deactivate")
                            .queryParam("sendEmail", false)
                            .build(userId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() != 404) {
                handleOktaError("Okta user deactivation failed.", ex);
            }
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach Okta management API.", ex);
        }

        try {
            restClient().delete()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/users/{id}")
                            .queryParam("sendEmail", false)
                            .build(userId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            handleOktaError("Okta user deletion failed.", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach Okta management API.", ex);
        }
    }

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(normalizeOrgUrl(orgUrl))
                .defaultHeader("Authorization", "SSWS " + apiToken.trim())
                .build();
    }

    private void validateConfiguration() {
        if (orgUrl == null || orgUrl.isBlank() || apiToken == null || apiToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Okta management is not configured. Set okta.management.org-url and okta.management.api-token."
            );
        }
    }

    private String normalizeOrgUrl(String value) {
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private void handleOktaError(String message, RestClientResponseException ex) {
        if (ex.getStatusCode().value() == 404) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Okta user not found.", ex);
        }
        if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Okta management credentials were rejected.", ex);
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message, ex);
    }

    public record CreatedOktaUser(String id, String email) {}
}
