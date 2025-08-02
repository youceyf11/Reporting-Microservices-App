package org.project.emailservice.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.util.List;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.dto.EmailAttachment;
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
    

     /**
     * Envoie un e-mail de rapport contenant un graphique en pièce jointe.
     * <p>
     * Attend une requête multipart :
     * <ul>
     *   <li><strong>request</strong> : métadonnées de l’e-mail au format JSON ({@link org.project.emailservice.dto.EmailRequest}).</li>
     *   <li><strong>file</strong> : image du graphique à joindre.</li>
     * </ul>
     * @param meta métadonnées de l’e-mail (partie « request »)
     * @param file fichier image du graphique (partie « file »)
     * @return <code>Mono</code> émettant une réponse HTTP 202 avec {@link org.project.emailservice.dto.EmailResponse}
     */
    @PostMapping(value = "/send/chart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<EmailResponse>> sendChartEmail(
            @RequestPart("request") @Valid EmailRequest meta,
            @RequestPart("file") MultipartFile file) {

        byte[] chartData;
        try {
            chartData = file.getBytes();
        } catch (Exception e) {
            return Mono.error(e);
        }

        EmailRequest request = meta.toBuilder()
                .subject(meta.getSubject() != null ? meta.getSubject()
                        : "Chart Report - " + meta.getTemplateData().getOrDefault("projectKey", ""))
                .templateName("chart-email")
                .attachments(List.of(
                        EmailAttachment.builder()
                                .filename(meta.getTemplateData() != null
                                        ? meta.getTemplateData().getOrDefault("chartType", "chart") + "-" +
                                          meta.getTemplateData().getOrDefault("projectKey", "proj") + ".png"
                                        : file.getOriginalFilename())
                                .content(chartData)
                                .contentType(file.getContentType() != null ? file.getContentType() : "image/png")
                                .build()))
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