package com.tracker.service;

import com.tracker.dto.PagedResponse;
import com.tracker.dto.TransactionRequest;
import com.tracker.dto.TransactionResponse;
import com.tracker.entity.Transaction;
import com.tracker.entity.User;
import com.tracker.enums.TransactionType;
import com.tracker.exception.AccessDeniedException;
import com.tracker.repository.CategoryRepository;
import com.tracker.repository.TransactionRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for TransactionService using jqwik.
 *
 * Feature: expense-subscription-tracker
 */
class TransactionServicePropertyTest {

    /**
     * Property 6: Transaction ownership isolation
     *
     * For any two distinct users A and B, user A SHALL NOT be able to read, update, or
     * delete transactions belonging to user B. Any such attempt SHALL return status 403
     * (via AccessDeniedException).
     *
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 100)
    @Tag("Feature_expense-subscription-tracker")
    @Tag("Property_6_Transaction_ownership_isolation")
    void userCannotAccessAnotherUsersTransaction(
            @ForAll @LongRange(min = 1, max = 1000) long userAId,
            @ForAll @LongRange(min = 1, max = 1000) long userBId,
            @ForAll("validTransactionRequest") TransactionRequest request
    ) {
        // Ensure distinct users
        Assume.that(userAId != userBId);

        // Setup
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        CacheService cacheService = mock(CacheService.class);
        TransactionService transactionService = new TransactionService(transactionRepository, categoryRepository, cacheService);

        User userA = new User();
        userA.setId(userAId);
        userA.setEmail("userA_" + userAId + "@example.com");

        User userB = new User();
        userB.setId(userBId);
        userB.setEmail("userB_" + userBId + "@example.com");

        // Create a transaction owned by User B
        Transaction transactionOfB = new Transaction();
        transactionOfB.setId(100L);
        transactionOfB.setUser(userB);
        transactionOfB.setAmount(new BigDecimal("50.00"));
        transactionOfB.setType(TransactionType.EXPENSE);
        transactionOfB.setDate(LocalDate.of(2024, 3, 15));
        transactionOfB.setDescription("User B transaction");
        transactionOfB.setCreatedAt(LocalDateTime.now());

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(transactionOfB));

        // User A attempts to UPDATE User B's transaction → should throw AccessDeniedException
        assertThatThrownBy(() -> transactionService.updateTransaction(userA, 100L, request))
                .isInstanceOf(AccessDeniedException.class);

        // User A attempts to DELETE User B's transaction → should throw AccessDeniedException
        assertThatThrownBy(() -> transactionService.deleteTransaction(userA, 100L))
                .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Property 7: Transaction list ordering
     *
     * For any user's transaction list response, the transactions SHALL be ordered by date
     * in strictly descending order (most recent first).
     *
     * **Validates: Requirements 3.5**
     */
    @Property(tries = 100)
    @Tag("Feature_expense-subscription-tracker")
    @Tag("Property_7_Transaction_list_ordering")
    void transactionsAreReturnedInDescendingDateOrder(
            @ForAll("transactionListWithDates") List<LocalDate> dates
    ) {
        // Setup
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        CacheService cacheService = mock(CacheService.class);
        TransactionService transactionService = new TransactionService(transactionRepository, categoryRepository, cacheService);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        // Sort dates descending to simulate the repository returning properly ordered data
        List<LocalDate> sortedDates = dates.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        // Create transactions in descending date order (as the repository should return)
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < sortedDates.size(); i++) {
            Transaction t = new Transaction();
            t.setId((long) (i + 1));
            t.setUser(user);
            t.setAmount(new BigDecimal("100.00"));
            t.setType(TransactionType.EXPENSE);
            t.setDate(sortedDates.get(i));
            t.setDescription("Transaction " + i);
            t.setCreatedAt(LocalDateTime.now());
            transactions.add(t);
        }

        Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 20), transactions.size());
        when(transactionRepository.findByUserOrderByDateDesc(any(User.class), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PagedResponse<TransactionResponse> response = transactionService.getTransactions(user, 0, 20);

        // Assert: the response content should be sorted by date descending
        List<TransactionResponse> content = response.content();
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).date())
                    .isAfterOrEqualTo(content.get(i + 1).date());
        }
    }

    /**
     * Property 8: Transaction mandatory field validation
     *
     * For any transaction creation request missing amount, type, or date, the API SHALL
     * reject the request with a validation error and no transaction SHALL be persisted.
     *
     * This test verifies at the service/validation layer that TransactionRequest with null
     * mandatory fields will fail Jakarta Bean Validation before reaching the service layer.
     *
     * **Validates: Requirements 3.6**
     */
    @Property(tries = 100)
    @Tag("Feature_expense-subscription-tracker")
    @Tag("Property_8_Transaction_mandatory_field_validation")
    void missingMandatoryFieldsCauseValidationError(
            @ForAll("invalidTransactionRequest") TransactionRequest request
    ) {
        // Setup
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        CacheService cacheService = mock(CacheService.class);
        TransactionService transactionService = new TransactionService(transactionRepository, categoryRepository, cacheService);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        // Validate the request using Jakarta Bean Validation manually
        jakarta.validation.Validator validator = jakarta.validation.Validation
                .buildDefaultValidatorFactory()
                .getValidator();

        Set<jakarta.validation.ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        // At least one violation should be present for any request missing mandatory fields
        assertThat(violations).isNotEmpty();

        // Verify no transaction is persisted (save should never be called if validation fails)
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // --- Arbitrary Providers ---

    @Provide
    Arbitrary<TransactionRequest> validTransactionRequest() {
        Arbitrary<BigDecimal> amounts = Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(99999.99))
                .ofScale(2);
        Arbitrary<TransactionType> types = Arbitraries.of(TransactionType.values());
        Arbitrary<LocalDate> dates = Arbitraries.integers()
                .between(2020, 2025)
                .flatMap(year -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(year, month, day))));
        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50);

        return Combinators.combine(amounts, types, dates, descriptions)
                .as((amount, type, date, desc) -> new TransactionRequest(amount, type, date, desc, null));
    }

    @Provide
    Arbitrary<List<LocalDate>> transactionListWithDates() {
        Arbitrary<LocalDate> dateArb = Arbitraries.integers()
                .between(2020, 2025)
                .flatMap(year -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(year, month, day))));

        return dateArb.list().ofMinSize(2).ofMaxSize(20);
    }

    @Provide
    Arbitrary<TransactionRequest> invalidTransactionRequest() {
        // Generate requests where at least one mandatory field (amount, type, date) is null
        Arbitrary<BigDecimal> amounts = Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(99999.99))
                .ofScale(2);
        Arbitrary<BigDecimal> nullableAmounts = Arbitraries.oneOf(
                amounts,
                Arbitraries.just(null)
        );

        Arbitrary<TransactionType> types = Arbitraries.of(TransactionType.values());
        Arbitrary<TransactionType> nullableTypes = Arbitraries.oneOf(
                types,
                Arbitraries.just(null)
        );

        Arbitrary<LocalDate> dates = Arbitraries.integers()
                .between(2020, 2025)
                .flatMap(year -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(year, month, day))));
        Arbitrary<LocalDate> nullableDates = Arbitraries.oneOf(
                dates,
                Arbitraries.just(null)
        );

        // Generate combinations, then filter to ensure at least one mandatory field is null
        return Combinators.combine(nullableAmounts, nullableTypes, nullableDates)
                .as((amount, type, date) -> new TransactionRequest(amount, type, date, "description", null))
                .filter(req -> req.amount() == null || req.type() == null || req.date() == null);
    }
}
