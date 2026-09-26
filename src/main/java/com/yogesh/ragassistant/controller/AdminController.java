package com.yogesh.ragassistant.controller;

import com.yogesh.ragassistant.dto.response.AdminStatsResponse;
import com.yogesh.ragassistant.dto.response.ApiResponse;
import com.yogesh.ragassistant.dto.response.DocumentResponse;
import com.yogesh.ragassistant.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Admin-only endpoints for system metrics, analytics, and document audits")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @Operation(summary = "Get system statistics", description = "Returns aggregated metrics including users, documents, chunks, queries, and provider configurations")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getSystemStats() {
        AdminStatsResponse stats = adminService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/documents")
    @Operation(summary = "Get all documents for audit", description = "Returns complete document list with status, error logs, and chunk counts")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getAdminDocuments() {
        List<DocumentResponse> documents = adminService.getAdminDocuments();
        return ResponseEntity.ok(ApiResponse.success(documents));
    }
}
