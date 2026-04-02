package com.racetrack.service;

import com.racetrack.model.User;

/**
 * Sends runner-facing notifications when coaches add comments.
 */
public interface CoachCommentNotificationService {

    /**
     * Sends a coach-comment notification when appropriate.
     *
     * @param athlete athlete receiving the notification
     * @param logType human-readable log type
     * @param comment coach comment text
     */
    void sendCoachCommentNotification(User athlete, String logType, String comment);
}
