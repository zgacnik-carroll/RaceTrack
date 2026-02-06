package com.racetrack.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Public home page";
    }

    @GetMapping("/secure")
    public String secure() {
        return "Welcome to RaceTrack!";
    }
}
