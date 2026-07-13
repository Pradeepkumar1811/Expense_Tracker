package com.tracker.service;

import com.tracker.dto.DashboardResponse;
import com.tracker.dto.SubscriptionResponse;
import com.tracker.entity.Budget;
import com.tracker.entity.Subscription;
import com.tracker.entity.User;
import com.tracker.enums.TransactionType;
import com.tracker.repository.BudgetRepository;
import com.tracker.repository.SubscriptionRepository;
import com.tracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregates dashboard data from transactions, subscriptions, and budgets.
 * Uses CacheService to cache responses in Redis with a 5-minute TTL.
 */
@Service
public class DashboardService {

    private static final String CACHE_KEY_PREFIX = "dashboard:";

    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BudgetRepository budgetRepository;
    private final CacheService cacheService;

    public DashboardService(TransactionRepository transactionRepository,
                            SubscriptionRepository subscriptionRepository,
                            BudgetRepository budgetRepository,
                            CacheService cacheService) {
        this.transactionRepository = transactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.budgetRepository = budgetRepository;
        this.cacheService = cacheService;
    }

    /**
     * Returns dashboard summary data for the given user.
     * Checks Redis cache first; if miss, computes from DB and caches the result.
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(User user) {
        String cacheKey = CACHE_KEY_PREFIX + user.getId();

        // Try cache first
        Object cached = cacheService.get(cacheKey);
        if (cached instanceof DashboardResponse dashboardResponse) {
            return dashboardResponse;
        }

        // Compute dashboard data
        DashboardResponse response = computeDashboard(user);

        // Cache with default 5-minute TTL
        cacheService.set(cacheKey, response);

        return response;
    }

    private DashboardResponse computeDashboard(User user) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // Current month income and expenses
        BigDecimal currentMonthIncome = transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                user, TransactionType.INCOME, monthStart, monthEnd);
        BigDecimal currentMonthExpenses = transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                user, TransactionType.EXPENSE, monthStart, monthEnd);
        BigDecimal netBalance = currentMonthIncome.subtract(currentMonthExpenses);

        // Active subscriptions count and total cost
        List<Subscription> subscriptions = subscriptionRepository.findByUser(user);
        int activeSubscriptionCount = subscriptions.size();
        BigDecimal totalSubscriptionCost = subscriptions.stream()
                .map(Subscription::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Budget utilization percentage for current month
        BigDecimal budgetUtilizationPercent = calculateBudgetUtilization(
                user, currentMonth.getMonthValue(), currentMonth.getYear(), currentMonthExpenses);

        // Upcoming renewals within 7 days
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);
        List<Subscription> upcomingSubscriptions = subscriptionRepository
                .findByUserAndNextRenewalDateBetween(user, today, sevenDaysLater);
        List<SubscriptionResponse> upcomingRenewals = upcomingSubscriptions.stream()
                .map(this::toSubscriptionResponse)
                .collect(Collectors.toList());

        return new DashboardResponse(
                currentMonthIncome,
                currentMonthExpenses,
                netBalance,
                activeSubscriptionCount,
                totalSubscriptionCost,
                budgetUtilizationPercent,
                upcomingRenewals
        );
    }

    private BigDecimal calculateBudgetUtilization(User user, int month, int year, BigDecimal totalExpenses) {
        // Find the overall monthly budget (no category)
        List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(user, month, year);

        // Look for an overall budget (category is null)
        BigDecimal totalBudgetLimit = budgets.stream()
                .map(Budget::getLimitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalBudgetLimit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Utilization = (totalExpenses / totalBudgetLimit) * 100
        return totalExpenses
                .multiply(BigDecimal.valueOf(100))
                .divide(totalBudgetLimit, 2, RoundingMode.HALF_UP);
    }

    private SubscriptionResponse toSubscriptionResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getName(),
                subscription.getAmount(),
                subscription.getBillingCycle(),
                subscription.getNextRenewalDate()
        );
    }
}
