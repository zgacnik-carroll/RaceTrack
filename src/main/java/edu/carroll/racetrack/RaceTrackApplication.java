package edu.carroll.racetrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the RaceTrack Spring Boot service.
 */
@SpringBootApplication
public class RaceTrackApplication {

    /**
     * Boots the Spring application context.
     *
     * @param args process arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(RaceTrackApplication.class, args);
    }
}

