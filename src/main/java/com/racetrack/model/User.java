package com.racetrack.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;       // Okta subject, stable unique identifier

    private String email;    // Okta email
    private String fullName; // Okta full name

    private String role;     // "athlete" or "coach"

    // Constructors
    public User() {}

    public User(String id, String email, String fullName, String role, String team) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
