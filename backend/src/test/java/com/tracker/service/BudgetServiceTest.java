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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private BudgetService budgetService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        category = new Category();
        category.setId(10L);
        category.setName("Food");
    }

    @Test
    void createBudget_overallBudget_success() {
        BudgetRequest request = new BudgetRequest(3, 2024, new BigDecimal("1000.00"), null);

        when(budgetRepository.findByUserAndMonthAndYearAndCategoryIsNull(user, 3, 2024))
                .thenReturn(Optional.empty());

        Budget savedBudget = createBudgetEntity(1L, user, 3, 2024, new BigDecimal("1000.00"), null);
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 3, 31))))
                .thenReturn(new BigDecimal("400.00"));

        BudgetStatusResponse response = budgetService.createBudget(user, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.month()).isEqualTo(3);
        assertThat(response.year()).isEqualTo(2024);
        assertThat(response.limitAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.totalSpending()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(response.remainingAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(response.overspent()).isFalse();
        assertThat(response.overspentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.categoryName()).isNull();
    }

    @Test
    void createBudget_categoryBudget_success() {
        BudgetRequest request = new BudgetRequest(3, 2024, new BigDecimal("500.00"), 10L);

        when(budgetRepository.findByUserAndMonthAndYearAndCategoryId(user, 3, 2024, 10L))
                .thenReturn(Optional.empty());
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        Budget savedBudget = createBudgetEntity(2L, user, 3, 2024, new BigDecimal("500.00"), category);
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetweenAndCategoryId(
                eq(user), eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 3, 31)), eq(10L)))
                .thenReturn(new BigDecimal("200.00"));

        BudgetStatusResponse response = budgetService.createBudget(user, request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.limitAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.totalSpending()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.remainingAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.overspent()).isFalse();
        assertThat(response.categoryName()).isEqualTo("Food");
    }

    @Test
    void createBudget_overspent_detected() {
        BudgetRequest request = new BudgetRequest(5, 2024, new BigDecimal("500.00"), null);

        when(budgetRepository.findByUserAndMonthAndYearAndCategoryIsNull(user, 5, 2024))
                .thenReturn(Optional.empty());

        Budget savedBudget = createBudgetEntity(3L, user, 5, 2024, new BigDecimal("500.00"), null);
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2024, 5, 1)), eq(LocalDate.of(2024, 5, 31))))
                .thenReturn(new BigDecimal("750.00"));

        BudgetStatusResponse response = budgetService.createBudget(user, request);

        assertThat(response.overspent()).isTrue();
        assertThat(response.overspentAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(response.remainingAmount()).isEqualByComparingTo(new BigDecimal("-250.00"));
    }

    @Test
    void createBudget_duplicate_throwsException() {
        BudgetRequest request = new BudgetRequest(3, 2024, new BigDecimal("1000.00"), null);

        Budget existing = createBudgetEntity(1L, user, 3, 2024, new BigDecimal("800.00"), null);
        when(budgetRepository.findByUserAndMonthAndYearAndCategoryIsNull(user, 3, 2024))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> budgetService.createBudget(user, request))
                .isInstanceOf(DuplicateBudgetException.class);
    }

    @Test
    void createBudget_categoryNotFound_throwsException() {
        BudgetRequest request = new BudgetRequest(3, 2024, new BigDecimal("500.00"), 99L);

        when(budgetRepository.findByUserAndMonthAndYearAndCategoryId(user, 3, 2024, 99L))
                .thenReturn(Optional.empty());
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.createBudget(user, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateBudget_success() {
        BudgetRequest request = new BudgetRequest(3, 2024, new BigDecimal("1500.00"), null);

        Budget existingBudget = createBudgetEntity(1L, user, 3, 2024, new BigDecimal("1000.00"), null);
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(existingBudget));

        Budget savedBudget = createBudgetEntity(1L, user, 3, 2024, new BigDecimal("1500.00"), null);
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 3, 31))))
                .thenReturn(new BigDecimal("400.00"));

        BudgetStatusResponse response = budgetService.updateBudget(user, 1L, request);

        assertThat(response.limitAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(response.remainingAmount()).isEqualByComparingTo(new BigDecimal("1100.00"));
    }

    @Test
    void updateBudget_notOwned_throwsAccessDenied() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        Budget existingBudget = createBudgetEntity(1L, otherUser, 3, 2024, new BigDecimal("1000.00"), null);
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(existingBudget));

        BudgetRequest request = new BudgetRequest(3, 2024, new BigDecimal("1500.00"), null);

        assertThatThrownBy(() -> budgetService.updateBudget(user, 1L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateBudget_notFound_throwsException() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        BudgetRequest request = new BudgetRequest(3, 2024, new BigDecimal("1500.00"), null);

        assertThatThrownBy(() -> budgetService.updateBudget(user, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBudgetsByMonthAndYear_returnsAll() {
        Budget budget1 = createBudgetEntity(1L, user, 3, 2024, new BigDecimal("1000.00"), null);
        Budget budget2 = createBudgetEntity(2L, user, 3, 2024, new BigDecimal("500.00"), category);

        when(budgetRepository.findByUserAndMonthAndYear(user, 3, 2024))
                .thenReturn(List.of(budget1, budget2));

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 3, 31))))
                .thenReturn(new BigDecimal("600.00"));

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetweenAndCategoryId(
                eq(user), eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 3, 31)), eq(10L)))
                .thenReturn(new BigDecimal("200.00"));

        List<BudgetStatusResponse> responses = budgetService.getBudgetsByMonthAndYear(user, 3, 2024);

        assertThat(responses).hasSize(2);

        // Overall budget
        BudgetStatusResponse overall = responses.get(0);
        assertThat(overall.categoryName()).isNull();
        assertThat(overall.totalSpending()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(overall.remainingAmount()).isEqualByComparingTo(new BigDecimal("400.00"));

        // Category budget
        BudgetStatusResponse categoryBudget = responses.get(1);
        assertThat(categoryBudget.categoryName()).isEqualTo("Food");
        assertThat(categoryBudget.totalSpending()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(categoryBudget.remainingAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void getBudgetsByMonthAndYear_noSpending_returnsFullRemaining() {
        Budget budget = createBudgetEntity(1L, user, 6, 2024, new BigDecimal("2000.00"), null);

        when(budgetRepository.findByUserAndMonthAndYear(user, 6, 2024))
                .thenReturn(List.of(budget));

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.EXPENSE),
                eq(LocalDate.of(2024, 6, 1)), eq(LocalDate.of(2024, 6, 30))))
                .thenReturn(BigDecimal.ZERO);

        List<BudgetStatusResponse> responses = budgetService.getBudgetsByMonthAndYear(user, 6, 2024);

        assertThat(responses).hasSize(1);
        BudgetStatusResponse response = responses.get(0);
        assertThat(response.totalSpending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.remainingAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(response.overspent()).isFalse();
        assertThat(response.overspentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Budget createBudgetEntity(Long id, User user, Integer month, Integer year,
                                      BigDecimal limitAmount, Category category) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setUser(user);
        budget.setMonth(month);
        budget.setYear(year);
        budget.setLimitAmount(limitAmount);
        budget.setCategory(category);
        budget.setCreatedAt(LocalDateTime.now());
        return budget;
    }
}
