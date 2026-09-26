package com.yogesh.ragassistant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private UUID userId;

    private String email;

    private String fullName;

    private String role;

    @Builder.Default
    private String authType = "HTTP Basic";

    private String message;
}
