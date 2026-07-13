package com.tracker.service;

import com.tracker.dto.CategoryBreakdown;
import com.tracker.dto.MonthlyReportResponse;
import com.tracker.entity.Transaction;
import com.tracker.entity.User;
import com.tracker.enums.TransactionType;
import com.tracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(User user, int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                user, TransactionType.INCOME, startDate, endDate);

        BigDecimal totalExpenses = transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                user, TransactionType.EXPENSE, startDate, endDate);

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        List<CategoryBreakdown> categoryBreakdown = computeCategoryBreakdown(user, startDate, endDate);

        return new MonthlyReportResponse(
                month,
                year,
                totalIncome,
                totalExpenses,
                netSavings,
                categoryBreakdown
        );
    }

    private List<CategoryBreakdown> computeCategoryBreakdown(User user, LocalDate startDate, LocalDate endDate) {
        List<Transaction> expenseTransactions = transactionRepository.findByUserAndDateBetween(user, startDate, endDate)
                .stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .toList();

        if (expenseTransactions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, BigDecimal> categoryTotals = expenseTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory().getName() : "Uncategorized",
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        return categoryTotals.entrySet().stream()
                .map(entry -> new CategoryBreakdown(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> b.total().compareTo(a.total()))
                .toList();
    }
}
