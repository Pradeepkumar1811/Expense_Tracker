package com.tracker.controller;

import com.tracker.dto.MonthlyReportResponse;
import com.tracker.entity.User;
import com.tracker.repository.UserRepository;
import com.tracker.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    public ReportController(ReportService reportService, UserRepository userRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @RequestParam Integer month,
            @RequestParam Integer year) {
        User user = getAuthenticatedUser();
        MonthlyReportResponse report = reportService.getMonthlyReport(user, month, year);
        return ResponseEntity.ok(report);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getCredentials().toString();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
