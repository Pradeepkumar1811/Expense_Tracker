package com.tracker.controller;

import com.tracker.entity.User;
import com.tracker.repository.UserRepository;
import com.tracker.service.ExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;
    private final UserRepository userRepository;

    public ExportController(ExportService exportService, UserRepository userRepository) {
        this.exportService = exportService;
        this.userRepository = userRepository;
    }

    @GetMapping("/csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        User user = getAuthenticatedUser();
        String csvContent = exportService.generateCsv(user, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions_export.csv\"")
                .body(csvContent);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
