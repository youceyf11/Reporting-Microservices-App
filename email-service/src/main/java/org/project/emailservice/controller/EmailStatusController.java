package org.project.emailservice.controller;

import org.springframework.web.bind.annotation.*;
import org.project.emailservice.service.EmailService;
import org.project.emailservice.dto.EmailResponse;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import java.util.List;


@RestController
@RequestMapping("/api/email/status")
public class EmailStatusController {
    
    private final EmailService emailService;
    
    public EmailStatusController(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @GetMapping("/{emailId}")
    public Mono<ResponseEntity<EmailResponse>> getEmailStatus(@PathVariable String emailId) {
        return emailService.getEmailStatus(emailId)
        .map(ResponseEntity::ok)
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
    
    @PostMapping("/bulk")  
    public Flux<EmailResponse> getBulkEmailStatus(@RequestBody List<String> emailIds) {
        return emailService.getBulkEmailStatus(emailIds);
    }
    
}