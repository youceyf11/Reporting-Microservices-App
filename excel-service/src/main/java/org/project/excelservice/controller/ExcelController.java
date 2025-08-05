package org.project.excelservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import org.project.excelservice.service.ExcelSyncService;

@RestController
@Slf4j
@RequestMapping("/api/excel")
public class ExcelController {
    
    private final ExcelSyncService excelSyncService;

    public ExcelController(ExcelSyncService excelSyncService) {
        this.excelSyncService = excelSyncService;
    }

    @GetMapping("/{projectKey}")
    public Mono<ResponseEntity<String>> sync(@PathVariable String projectKey) {
        return excelSyncService.sync(projectKey)
                .map(result -> ResponseEntity.ok("Sync completed successfully"))
                .onErrorResume(error -> {
                    log.error("Error during sync: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to sync data for project: "+ projectKey));
                });
    }
}
