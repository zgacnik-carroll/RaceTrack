package edu.carroll.racetrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entity representing a workout log submitted by a user.
 */
@Entity
@Table(name = "workout_logs")
public class WorkoutLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String workoutType;

    @Column(columnDefinition = "TEXT")
    private String completionDetails;

    @Column(columnDefinition = "TEXT")
    private String workoutDescription;

    @Column(columnDefinition = "TEXT")
    private String actualPaces;

    @Column(columnDefinition = "TEXT")
    private String coachComment;

    private LocalDateTime logDate;

    /**
     * Creates an empty workout log.
     */
    public WorkoutLog() {
    }

    /**
     * Returns the workout log id.
     *
     * @return log id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the workout log id.
     *
     * @param id log id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the owning user.
     *
     * @return owning user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the owning user.
     *
     * @param user owning user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Returns the workout type.
     *
     * @return workout type
     */
    public String getWorkoutType() {
        return workoutType;
    }

    /**
     * Sets the workout type.
     *
     * @param workoutType workout type
     */
    public void setWorkoutType(String workoutType) {
        this.workoutType = workoutType;
    }

    /**
     * Returns completion details.
     *
     * @return completion details
     */
    public String getCompletionDetails() {
        return completionDetails;
    }

    /**
     * Sets completion details.
     *
     * @param completionDetails completion details
     */
    public void setCompletionDetails(String completionDetails) {
        this.completionDetails = completionDetails;
    }

    /**
     * Returns workout description.
     *
     * @return workout description
     */
    public String getWorkoutDescription() {
        return workoutDescription;
    }

    /**
     * Sets workout description.
     *
     * @param workoutDescription workout description
     */
    public void setWorkoutDescription(String workoutDescription) {
        this.workoutDescription = workoutDescription;
    }

    /**
     * Returns actual paces.
     *
     * @return actual paces
     */
    public String getActualPaces() {
        return actualPaces;
    }

    /**
     * Sets actual paces.
     *
     * @param actualPaces actual paces
     */
    public void setActualPaces(String actualPaces) {
        this.actualPaces = actualPaces;
    }

    /**
     * Returns coach comment.
     *
     * @return coach comment
     */
    public String getCoachComment() {
        return coachComment;
    }

    /**
     * Sets coach comment.
     *
     * @param coachComment coach comment
     */
    public void setCoachComment(String coachComment) {
        this.coachComment = coachComment;
    }

    /**
     * Returns the log date.
     *
     * @return log date
     */
    public LocalDateTime getLogDate() {
        return logDate;
    }

    /**
     * Sets the log date.
     *
     * @param logDate log date
     */
    public void setLogDate(LocalDateTime logDate) {
        this.logDate = logDate;
    }
}

