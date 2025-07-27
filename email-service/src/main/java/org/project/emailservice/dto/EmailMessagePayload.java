package org.project.emailservice.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailMessagePayload implements Serializable {
    private String emailId;
    private EmailRequest request;
}