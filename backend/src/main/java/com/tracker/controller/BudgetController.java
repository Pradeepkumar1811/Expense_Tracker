package com.tracker.controller;

import com.tracker.dto.BudgetRequest;
import com.tracker.dto.BudgetStatusResponse;
import com.tracker.entity.User;
import com.tracker.repository.UserRepository;
import com.tracker.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final UserRepository userRepository;

    public BudgetController(BudgetService budgetService, UserRepository userRepository) {
        this.budgetService = budgetService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<BudgetStatusResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        User user = getAuthenticatedUser();
        BudgetStatusResponse response = budgetService.createBudget(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetStatusResponse> updateBudget(@PathVariable Long id,
                                                             @Valid @RequestBody BudgetRequest request) {
        User user = getAuthenticatedUser();
        BudgetStatusResponse response = budgetService.updateBudget(user, id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BudgetStatusResponse>> getBudgets(@RequestParam Integer month,
                                                                 @RequestParam Integer year) {
        User user = getAuthenticatedUser();
        List<BudgetStatusResponse> response = budgetService.getBudgetsByMonthAndYear(user, month, year);
        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
