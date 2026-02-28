package com.edi.backend.controllers;

import com.edi.backend.services.SampleFolderIntakeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/intake")
public class IntakeController {

    private final SampleFolderIntakeService intakeService;

    public IntakeController(SampleFolderIntakeService intakeService) {
        this.intakeService = intakeService;
    }

    @PostMapping("/samples")
    public SampleFolderIntakeService.SampleIntakeResult intakeFromSampleFolder() {
        try {
            return intakeService.scanSampleFolder();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read sample folder", e);
        }
    }
}