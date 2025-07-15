package org.project.jirafetchservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("user")
public class User {
    @Id
    private UUID id;

    private String username;
    private String email;
    private String fullName;
}