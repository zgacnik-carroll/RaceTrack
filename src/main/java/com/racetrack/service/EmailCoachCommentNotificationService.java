package com.racetrack.service;

import com.racetrack.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email-backed coach-comment notifications.
 */
@Service
@ConditionalOnProperty(prefix = "racetrack.notifications.email", name = "enabled", havingValue = "true")
public class EmailCoachCommentNotificationService implements CoachCommentNotificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailCoachCommentNotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String subjectPrefix;

    public EmailCoachCommentNotificationService(JavaMailSender mailSender,
                                                @Value("${racetrack.notifications.email.from}") String fromAddress,
                                                @Value("${racetrack.notifications.email.subject-prefix:[RaceTrack]}") String subjectPrefix) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.subjectPrefix = subjectPrefix;
    }

    @Override
    public void sendCoachCommentNotification(User athlete, String logType, String comment) {
        if (athlete == null || athlete.getEmail() == null || athlete.getEmail().isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(athlete.getEmail());
        message.setSubject(subjectPrefix + " New coach comment on your " + logType);
        message.setText(buildBody(athlete, logType, comment));

        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            log.warn("Failed to send coach comment email to {}", athlete.getEmail(), ex);
        }
    }

    private String buildBody(User athlete, String logType, String comment) {
        String athleteName = athlete.getFullName() != null && !athlete.getFullName().isBlank()
                ? athlete.getFullName()
                : athlete.getEmail();
        return "Hi " + athleteName + ",\n\n"
                + "Your coach left a new comment on your " + logType + ":\n\n"
                + comment + "\n\n"
                + "Log in to RaceTrack to review it.";
    }
}
