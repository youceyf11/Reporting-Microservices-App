package org.project.emailservice.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {
    private String id;
    private String name;
    private String subject;
    private String htmlContent;
    private String textContent;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean active;
}
