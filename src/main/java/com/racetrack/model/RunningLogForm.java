package com.racetrack.model;

public class RunningLogForm {

    // Mileage
    private Double mileage;

    // Injury / hurting
    private Boolean hurting;
    private String injuryDescription;

    // Recovery & wellness
    private Integer sleepHours;
    private Integer stressLevel; // 1–10

    // Nutrition
    private Boolean plateProportion;
    private Boolean gotThatBread;

    // Perceived effort & feel
    private String feel;
    private Integer rpe; // 1–10

    // Run description
    private String details;

    // Getters & Setters

    public Double getMileage() {
        return mileage;
    }

    public void setMileage(Double mileage) {
        this.mileage = mileage;
    }

    public Boolean getHurting() {
        return hurting;
    }

    public void setHurting(Boolean hurting) {
        this.hurting = hurting;
    }

    public String getInjuryDescription() {
        return injuryDescription;
    }

    public void setInjuryDescription(String injuryDescription) {
        this.injuryDescription = injuryDescription;
    }

    public Integer getSleepHours() {
        return sleepHours;
    }

    public void setSleepHours(Integer sleepHours) {
        this.sleepHours = sleepHours;
    }

    public Integer getStressLevel() {
        return stressLevel;
    }

    public void setStressLevel(Integer stressLevel) {
        this.stressLevel = stressLevel;
    }

    public Boolean getPlateProportion() {
        return plateProportion;
    }

    public void setPlateProportion(Boolean plateProportion) {
        this.plateProportion = plateProportion;
    }

    public Boolean getGotThatBread() {
        return gotThatBread;
    }

    public void setGotThatBread(Boolean gotThatBread) {
        this.gotThatBread = gotThatBread;
    }

    public String getFeel() {
        return feel;
    }

    public void setFeel(String feel) {
        this.feel = feel;
    }

    public Integer getRpe() {
        return rpe;
    }

    public void setRpe(Integer rpe) {
        this.rpe = rpe;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
