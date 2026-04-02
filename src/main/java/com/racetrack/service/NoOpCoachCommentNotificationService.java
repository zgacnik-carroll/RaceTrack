package com.racetrack.service;

import com.racetrack.model.User;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Fallback notification service when email notifications are disabled.
 */
@Service
@ConditionalOnProperty(prefix = "racetrack.notifications.email", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpCoachCommentNotificationService implements CoachCommentNotificationService {

    @Override
    public void sendCoachCommentNotification(User athlete, String logType, String comment) {
        // Intentionally no-op when notifications are disabled.
    }
}
