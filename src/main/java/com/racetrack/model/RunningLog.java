package com.racetrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Entity representing a daily running log submitted by a user.
 */
@Entity
@Table(name = "running_logs")
public class RunningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to the runner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Mileage
    @Column(nullable = false)
    private Double mileage;

    // Injury / hurting
    private Boolean hurting;

    // Recovery & wellness
    private Integer sleepHours;
    private Integer stressLevel; // 1-10

    // Nutrition
    private Boolean plateProportion;
    private Boolean gotThatBread;

    // Perceived effort & feel
    private String feel;
    private Integer rpe; // 1-10

    // Run description
    @Column(columnDefinition = "TEXT", nullable = false)
    private String details;

    @Column(columnDefinition = "TEXT")
    private String coachComment;

    // Automatically store the date/time of submission
    @CreationTimestamp
    private LocalDateTime logDate;

    /**
     * Creates an empty running log.
     */
    public RunningLog() {}

    /**
     * Returns the running log id.
     *
     * @return log id
     */
    public Long getId() { return id; }

    /**
     * Sets the running log id.
     *
     * @param id log id
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns the owning user.
     *
     * @return owning user
     */
    public User getUser() { return user; }

    /**
     * Sets the owning user.
     *
     * @param user owning user
     */
    public void setUser(User user) { this.user = user; }

    /**
     * Returns mileage.
     *
     * @return mileage
     */
    public Double getMileage() { return mileage; }

    /**
     * Sets mileage.
     *
     * @param mileage mileage
     */
    public void setMileage(Double mileage) { this.mileage = mileage; }

    /**
     * Returns hurting flag.
     *
     * @return hurting value
     */
    public Boolean getHurting() { return hurting; }

    /**
     * Sets hurting flag.
     *
     * @param hurting hurting value
     */
    public void setHurting(Boolean hurting) { this.hurting = hurting; }

    /**
     * Returns sleep hours.
     *
     * @return sleep hours
     */
    public Integer getSleepHours() { return sleepHours; }

    /**
     * Sets sleep hours.
     *
     * @param sleepHours sleep hours
     */
    public void setSleepHours(Integer sleepHours) { this.sleepHours = sleepHours; }

    /**
     * Returns stress level.
     *
     * @return stress level
     */
    public Integer getStressLevel() { return stressLevel; }

    /**
     * Sets stress level.
     *
     * @param stressLevel stress level
     */
    public void setStressLevel(Integer stressLevel) { this.stressLevel = stressLevel; }

    /**
     * Returns plate proportion flag.
     *
     * @return plate proportion value
     */
    public Boolean getPlateProportion() { return plateProportion; }

    /**
     * Sets plate proportion flag.
     *
     * @param plateProportion plate proportion value
     */
    public void setPlateProportion(Boolean plateProportion) { this.plateProportion = plateProportion; }

    /**
     * Returns got-that-bread flag.
     *
     * @return got-that-bread value
     */
    public Boolean getGotThatBread() { return gotThatBread; }

    /**
     * Sets got-that-bread flag.
     *
     * @param gotThatBread got-that-bread value
     */
    public void setGotThatBread(Boolean gotThatBread) { this.gotThatBread = gotThatBread; }

    /**
     * Returns feel value.
     *
     * @return feel text
     */
    public String getFeel() { return feel; }

    /**
     * Sets feel value.
     *
     * @param feel feel text
     */
    public void setFeel(String feel) { this.feel = feel; }

    /**
     * Returns RPE score.
     *
     * @return RPE score
     */
    public Integer getRpe() { return rpe; }

    /**
     * Sets RPE score.
     *
     * @param rpe RPE score
     */
    public void setRpe(Integer rpe) { this.rpe = rpe; }

    /**
     * Returns details text.
     *
     * @return details text
     */
    public String getDetails() { return details; }

    /**
     * Sets details text.
     *
     * @param details details text
     */
    public void setDetails(String details) { this.details = details; }

    /**
     * Returns coach comment text.
     *
     * @return coach comment
     */
    public String getCoachComment() { return coachComment; }

    /**
     * Sets coach comment text.
     *
     * @param coachComment coach comment
     */
    public void setCoachComment(String coachComment) { this.coachComment = coachComment; }

    /**
     * Returns log date.
     *
     * @return log date
     */
    public LocalDateTime getLogDate() { return logDate; }

    /**
     * Sets log date.
     *
     * @param logDate log date
     */
    public void setLogDate(LocalDateTime logDate) { this.logDate = logDate; }

    /**
     * Returns a debug representation of this running log.
     *
     * @return log debug string
     */
    @Override
    public String toString() {
        return "RunningLog{" +
                "id=" + id +
                ", user=" + (user != null ? user.getEmail() : "null") +
                ", mileage=" + mileage +
                ", hurting=" + hurting +
                ", sleepHours=" + sleepHours +
                ", stressLevel=" + stressLevel +
                ", plateProportion=" + plateProportion +
                ", gotThatBread=" + gotThatBread +
                ", feel='" + feel + '\'' +
                ", rpe=" + rpe +
                ", details='" + details + '\'' +
                ", coachComment='" + coachComment + '\'' +
                ", logDate=" + logDate +
                '}';
    }
}
