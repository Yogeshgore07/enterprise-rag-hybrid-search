package com.yogesh.ragassistant.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetails {

    private int status;

    private String error;

    private String message;

    private String path;

    private List<String> validationErrors;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
