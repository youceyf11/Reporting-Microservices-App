package org.project.emailservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailAttachment {
    private String filename;
    private byte[] content;
    private String contentType;
    private long size;
}
