package com.notifyhub.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("users")
public class User {
    @Id
    private Long id;
    private String username;
    private String email;
    private String password;
    @Builder.Default
    private String role = "USER";
    private String telegramChatId;
    private LocalDateTime createdAt;
}
