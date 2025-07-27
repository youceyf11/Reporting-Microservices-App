package org.project.emailservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.util.List;
import java.util.Map;
import java.time.Instant;   
import org.project.emailservice.enums.EmailPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String id;
    
    @NotBlank(message = "Email recipient is required")
    @Email(message = "Invalid email format")
    private String to;
    
    private String from;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    private String templateName;
    private Map<String, Object> templateData;
    private List<EmailAttachment> attachments;
    private EmailPriority priority;
    private Instant scheduledAt;
    private Map<String, String> metadata;
}
