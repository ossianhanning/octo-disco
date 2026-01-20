package com.voyado.generator.controller;

import com.voyado.generator.model.GeneratorStats;
import com.voyado.generator.service.LoadGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/generator")
public class GeneratorController {

    private static final Logger log = LoggerFactory.getLogger(GeneratorController.class);

    private final LoadGeneratorService loadGeneratorService;

    public GeneratorController(LoadGeneratorService loadGeneratorService) {
        this.loadGeneratorService = loadGeneratorService;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startGeneration(
            @RequestParam(defaultValue = "1000000") long totalEvents,
            @RequestParam(defaultValue = "50") int searchPercentage) {
        
        if (loadGeneratorService.isRunning()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Load generation already running"));
        }

        loadGeneratorService.startGeneration(totalEvents, searchPercentage);
        
        return ResponseEntity.ok(Map.of(
                "message", "Load generation started",
                "totalEvents", String.valueOf(totalEvents),
                "searchPercentage", searchPercentage + "%"
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stopGeneration() {
        if (!loadGeneratorService.isRunning()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Load generation not running"));
        }

        loadGeneratorService.stopGeneration();
        return ResponseEntity.ok(Map.of("message", "Load generation stopped"));
    }

    @GetMapping("/stats")
    public ResponseEntity<GeneratorStats> getStats() {
        return ResponseEntity.ok(loadGeneratorService.getStats());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        GeneratorStats stats = loadGeneratorService.getStats();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "running", stats.isRunning(),
                "totalEvents", stats.getTotalEvents()
        ));
    }
}
