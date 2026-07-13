package com.tracker.controller;

import com.tracker.dto.DashboardResponse;
import com.tracker.entity.User;
import com.tracker.repository.UserRepository;
import com.tracker.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    public DashboardController(DashboardService dashboardService, UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard() {
        User user = getAuthenticatedUser();
        DashboardResponse response = dashboardService.getDashboard(user);
        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
