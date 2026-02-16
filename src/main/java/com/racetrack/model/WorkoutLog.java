package com.racetrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workout_logs")
public class WorkoutLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to the runner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Strength / Strides / Assigned Work
    private String workoutType;

    // What was completed
    @Column(columnDefinition = "TEXT")
    private String completionDetails;

    // Description & pacing
    @Column(columnDefinition = "TEXT")
    private String workoutDescription;

    @Column(columnDefinition = "TEXT")
    private String actualPaces;

    // Automatically store the date/time of submission
    private LocalDateTime logDate;

    // Constructors
    public WorkoutLog() {
        this.logDate = LocalDateTime.now(); // default to now
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getWorkoutType() { return workoutType; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }

    public String getCompletionDetails() { return completionDetails; }
    public void setCompletionDetails(String completionDetails) { this.completionDetails = completionDetails; }

    public String getWorkoutDescription() { return workoutDescription; }
    public void setWorkoutDescription(String workoutDescription) { this.workoutDescription = workoutDescription; }

    public String getActualPaces() { return actualPaces; }
    public void setActualPaces(String actualPaces) { this.actualPaces = actualPaces; }

    public LocalDateTime getLogDate() { return logDate; }
    public void setLogDate(LocalDateTime logDate) { this.logDate = logDate; }
}
