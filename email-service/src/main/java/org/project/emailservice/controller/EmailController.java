package org.project.emailservice.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.util.Map;
import java.util.List;
import java.time.Instant;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.dto.EmailAttachment;
import org.project.emailservice.enums.EmailPriority;
import org.project.emailservice.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/api/emails")
@Slf4j
public class EmailController {
    
    private final EmailService emailService;
    
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @PostMapping("/send")
    public Mono<ResponseEntity<EmailResponse>> sendEmail(
            @RequestBody @Valid EmailRequest request) {
        
        return emailService.processEmailRequest(request)
                .map(response -> ResponseEntity.accepted().body(response));
    }
    
    @PostMapping("/send/chart")
    public Mono<ResponseEntity<EmailResponse>> sendChartEmail(
            @RequestParam String to,
            @RequestParam String projectKey,
            @RequestParam String chartType,
            @RequestBody byte[] chartData) {
        
        EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Chart Report - " + projectKey)
                .templateName("chart-email")
                .templateData(Map.of(
                    "projectKey", projectKey,
                    "chartType", chartType,
                    "generatedAt", Instant.now()
                ))
                .attachments(List.of(
                    EmailAttachment.builder()
                        .filename(chartType + "-" + projectKey + ".png")
                        .content(chartData)
                        .contentType("image/png")
                        .build()
                ))
                .priority(EmailPriority.NORMAL)
                .build();
        
        return emailService.processEmailRequest(request)
                .map(response -> ResponseEntity.accepted().body(response));
    }
    
    @GetMapping("/status/{emailId}")
    public Mono<ResponseEntity<EmailResponse>> getEmailStatus(
            @PathVariable String emailId) {
        
                return emailService.getEmailStatus(emailId)
                .map(emailLog -> {
                    String priority = (emailLog.getPriority() != null) ? emailLog.getPriority() : null;
                    EmailResponse response = new EmailResponse(
                        emailLog.getEmailId(),
                        emailLog.getStatus(),
                        emailLog.getErrorMessage(),
                        priority,
                        emailLog.getCreatedAt(),
                        emailLog.getUpdatedAt()
                    );
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/status/bulk")
    public Flux<EmailResponse> getBulkEmailStatus(
            @RequestParam List<String> emailIds) {
        
        return emailService.getBulkEmailStatus(emailIds);
    }
        
}