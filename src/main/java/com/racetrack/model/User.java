package com.racetrack.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing an authenticated user from the identity provider.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;       // Okta subject, stable unique identifier

    private String email;    // Okta email
    private String fullName; // Okta full name

    private String role;     // "athlete" or "coach"

    /**
     * Creates an empty user.
     */
    public User() {}

    /**
     * Creates a user record based on identity provider attributes.
     * The {@code team} parameter is retained for compatibility even if unused.
     *
     * @param id user id from identity provider
     * @param email user email
     * @param fullName user full name
     * @param role logical application role
     * @param team legacy constructor parameter kept for compatibility
     */
    public User(String id, String email, String fullName, String role, String team) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    /**
     * Returns the user id.
     *
     * @return user id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the user id.
     *
     * @param id user id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the user email.
     *
     * @return email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user email.
     *
     * @param email email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the user full name.
     *
     * @return full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the user full name.
     *
     * @param fullName full name
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Returns the application role.
     *
     * @return role value
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the application role.
     *
     * @param role role value
     */
    public void setRole(String role) {
        this.role = role;
    }
}
