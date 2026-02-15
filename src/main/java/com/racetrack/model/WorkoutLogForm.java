package com.racetrack.model;

public class WorkoutLogForm {

    // Strength / Strides / Assigned Work
    private String workoutType;

    // What was completed
    private String completionDetails;

    // Description & pacing
    private String workoutDescription;
    private String assignedPaces;
    private String actualPaces;

    // Getters & Setters

    public String getWorkoutType() {
        return workoutType;
    }

    public void setWorkoutType(String workoutType) {
        this.workoutType = workoutType;
    }

    public String getCompletionDetails() {
        return completionDetails;
    }

    public void setCompletionDetails(String completionDetails) {
        this.completionDetails = completionDetails;
    }

    public String getWorkoutDescription() {
        return workoutDescription;
    }

    public void setWorkoutDescription(String workoutDescription) {
        this.workoutDescription = workoutDescription;
    }

    public String getAssignedPaces() {
        return assignedPaces;
    }

    public void setAssignedPaces(String assignedPaces) {
        this.assignedPaces = assignedPaces;
    }

    public String getActualPaces() {
        return actualPaces;
    }

    public void setActualPaces(String actualPaces) {
        this.actualPaces = actualPaces;
    }
}
