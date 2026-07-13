package com.tracker.service;

import com.tracker.dto.BudgetRequest;
import com.tracker.dto.BudgetStatusResponse;
import com.tracker.entity.Budget;
import com.tracker.entity.Category;
import com.tracker.entity.User;
import com.tracker.enums.TransactionType;
import com.tracker.exception.AccessDeniedException;
import com.tracker.exception.DuplicateBudgetException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.BudgetRepository;
import com.tracker.repository.CategoryRepository;
import com.tracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private static final String DASHBOARD_CACHE_KEY_PREFIX = "dashboard:";

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CacheService cacheService;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository,
                         CategoryRepository categoryRepository,
                         CacheService cacheService) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.cacheService = cacheService;
    }

    @Transactional
    public BudgetStatusResponse createBudget(User user, BudgetRequest request) {
        // Check for duplicate budget
        Optional<Budget> existing;
        if (request.categoryId() != null) {
            existing = budgetRepository.findByUserAndMonthAndYearAndCategoryId(
                    user, request.month(), request.year(), request.categoryId());
        } else {
            existing = budgetRepository.findByUserAndMonthAndYearAndCategoryIsNull(
                    user, request.month(), request.year());
        }

        if (existing.isPresent()) {
            throw new DuplicateBudgetException(request.month(), request.year());
        }

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setMonth(request.month());
        budget.setYear(request.year());
        budget.setLimitAmount(request.limitAmount());
        budget.setCreatedAt(LocalDateTime.now());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            budget.setCategory(category);
        }

        Budget saved = budgetRepository.save(budget);
        cacheService.invalidate(DASHBOARD_CACHE_KEY_PREFIX + user.getId());
        return toBudgetStatusResponse(saved, user);
    }

    @Transactional
    public BudgetStatusResponse updateBudget(User user, Long budgetId, BudgetRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", budgetId));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException();
        }

        budget.setLimitAmount(request.limitAmount());
        budget.setMonth(request.month());
        budget.setYear(request.year());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            budget.setCategory(category);
        } else {
            budget.setCategory(null);
        }

        Budget saved = budgetRepository.save(budget);
        cacheService.invalidate(DASHBOARD_CACHE_KEY_PREFIX + user.getId());
        return toBudgetStatusResponse(saved, user);
    }

    @Transactional(readOnly = true)
    public List<BudgetStatusResponse> getBudgetsByMonthAndYear(User user, Integer month, Integer year) {
        List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(user, month, year);
        return budgets.stream()
                .map(budget -> toBudgetStatusResponse(budget, user))
                .collect(Collectors.toList());
    }

    private BudgetStatusResponse toBudgetStatusResponse(Budget budget, User user) {
        BigDecimal totalSpending = calculateTotalSpending(user, budget);
        BigDecimal limitAmount = budget.getLimitAmount();
        BigDecimal remainingAmount = limitAmount.subtract(totalSpending);
        boolean overspent = totalSpending.compareTo(limitAmount) > 0;
        BigDecimal overspentAmount = overspent ? totalSpending.subtract(limitAmount) : BigDecimal.ZERO;

        String categoryName = budget.getCategory() != null ? budget.getCategory().getName() : null;

        return new BudgetStatusResponse(
                budget.getId(),
                budget.getMonth(),
                budget.getYear(),
                limitAmount,
                totalSpending,
                remainingAmount,
                overspent,
                overspentAmount,
                categoryName
        );
    }

    private BigDecimal calculateTotalSpending(User user, Budget budget) {
        YearMonth yearMonth = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        if (budget.getCategory() != null) {
            return transactionRepository.sumAmountByUserAndTypeAndDateBetweenAndCategoryId(
                    user, TransactionType.EXPENSE, startDate, endDate, budget.getCategory().getId());
        } else {
            return transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                    user, TransactionType.EXPENSE, startDate, endDate);
        }
    }
}
