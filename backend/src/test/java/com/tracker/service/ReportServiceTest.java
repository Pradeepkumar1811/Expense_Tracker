package com.tracker.service;

import com.tracker.dto.CategoryBreakdown;
import com.tracker.dto.MonthlyReportResponse;
import com.tracker.entity.Category;
import com.tracker.entity.Transaction;
import com.tracker.entity.User;
import com.tracker.enums.TransactionType;
import com.tracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
    }

    @Test
    void getMonthlyReport_withTransactions_calculatesCorrectly() {
        LocalDate startDate = LocalDate.of(2024, 3, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                .thenReturn(new BigDecimal("5000.00"));

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                .thenReturn(new BigDecimal("3000.00"));

        Category food = createCategory(1L, "Food");
        Category rent = createCategory(2L, "Rent");

        Transaction expense1 = createTransaction(1L, user, new BigDecimal("800.00"), TransactionType.EXPENSE, LocalDate.of(2024, 3, 5), food);
        Transaction expense2 = createTransaction(2L, user, new BigDecimal("1200.00"), TransactionType.EXPENSE, LocalDate.of(2024, 3, 10), rent);
        Transaction expense3 = createTransaction(3L, user, new BigDecimal("1000.00"), TransactionType.EXPENSE, LocalDate.of(2024, 3, 15), food);
        Transaction income1 = createTransaction(4L, user, new BigDecimal("5000.00"), TransactionType.INCOME, LocalDate.of(2024, 3, 1), null);

        when(transactionRepository.findByUserAndDateBetween(eq(user), eq(startDate), eq(endDate)))
                .thenReturn(List.of(expense1, expense2, expense3, income1));

        MonthlyReportResponse report = reportService.getMonthlyReport(user, 3, 2024);

        assertThat(report.month()).isEqualTo(3);
        assertThat(report.year()).isEqualTo(2024);
        assertThat(report.totalIncome()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(report.totalExpenses()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(report.netSavings()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(report.categoryBreakdown()).hasSize(2);

        // Breakdown sorted by total descending: Food=1800, Rent=1200
        assertThat(report.categoryBreakdown().get(0).categoryName()).isEqualTo("Food");
        assertThat(report.categoryBreakdown().get(0).total()).isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(report.categoryBreakdown().get(1).categoryName()).isEqualTo("Rent");
        assertThat(report.categoryBreakdown().get(1).total()).isEqualByComparingTo(new BigDecimal("1200.00"));
    }

    @Test
    void getMonthlyReport_noTransactions_returnsZeroValues() {
        LocalDate startDate = LocalDate.of(2024, 6, 1);
        LocalDate endDate = LocalDate.of(2024, 6, 30);

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                .thenReturn(BigDecimal.ZERO);

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                .thenReturn(BigDecimal.ZERO);

        when(transactionRepository.findByUserAndDateBetween(eq(user), eq(startDate), eq(endDate)))
                .thenReturn(Collections.emptyList());

        MonthlyReportResponse report = reportService.getMonthlyReport(user, 6, 2024);

        assertThat(report.month()).isEqualTo(6);
        assertThat(report.year()).isEqualTo(2024);
        assertThat(report.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.totalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.netSavings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.categoryBreakdown()).isEmpty();
    }

    @Test
    void getMonthlyReport_onlyIncome_noExpenses() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                .thenReturn(new BigDecimal("3000.00"));

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                .thenReturn(BigDecimal.ZERO);

        Transaction income = createTransaction(1L, user, new BigDecimal("3000.00"), TransactionType.INCOME, LocalDate.of(2024, 1, 15), null);
        when(transactionRepository.findByUserAndDateBetween(eq(user), eq(startDate), eq(endDate)))
                .thenReturn(List.of(income));

        MonthlyReportResponse report = reportService.getMonthlyReport(user, 1, 2024);

        assertThat(report.totalIncome()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(report.totalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.netSavings()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(report.categoryBreakdown()).isEmpty();
    }

    @Test
    void getMonthlyReport_expensesWithoutCategory_groupedAsUncategorized() {
        LocalDate startDate = LocalDate.of(2024, 4, 1);
        LocalDate endDate = LocalDate.of(2024, 4, 30);

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                .thenReturn(BigDecimal.ZERO);

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                .thenReturn(new BigDecimal("200.00"));

        Transaction expense = createTransaction(1L, user, new BigDecimal("200.00"), TransactionType.EXPENSE, LocalDate.of(2024, 4, 10), null);
        when(transactionRepository.findByUserAndDateBetween(eq(user), eq(startDate), eq(endDate)))
                .thenReturn(List.of(expense));

        MonthlyReportResponse report = reportService.getMonthlyReport(user, 4, 2024);

        assertThat(report.categoryBreakdown()).hasSize(1);
        assertThat(report.categoryBreakdown().get(0).categoryName()).isEqualTo("Uncategorized");
        assertThat(report.categoryBreakdown().get(0).total()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void getMonthlyReport_februaryLeapYear_usesCorrectBoundaries() {
        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29); // 2024 is a leap year

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                .thenReturn(new BigDecimal("1000.00"));

        when(transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                eq(user), eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                .thenReturn(new BigDecimal("500.00"));

        when(transactionRepository.findByUserAndDateBetween(eq(user), eq(startDate), eq(endDate)))
                .thenReturn(Collections.emptyList());

        MonthlyReportResponse report = reportService.getMonthlyReport(user, 2, 2024);

        assertThat(report.month()).isEqualTo(2);
        assertThat(report.year()).isEqualTo(2024);
        assertThat(report.netSavings()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    private Category createCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private Transaction createTransaction(Long id, User user, BigDecimal amount,
                                          TransactionType type, LocalDate date, Category category) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDate(date);
        transaction.setCategory(category);
        return transaction;
    }
}
