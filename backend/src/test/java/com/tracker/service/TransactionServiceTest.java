package com.tracker.service;

import com.tracker.dto.PagedResponse;
import com.tracker.dto.TransactionRequest;
import com.tracker.dto.TransactionResponse;
import com.tracker.entity.Category;
import com.tracker.entity.Transaction;
import com.tracker.entity.User;
import com.tracker.enums.TransactionType;
import com.tracker.exception.AccessDeniedException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.CategoryRepository;
import com.tracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private TransactionService transactionService;

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
    void createTransaction_withoutCategory_success() {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("150.00"), TransactionType.EXPENSE, LocalDate.of(2024, 3, 15),
                "Grocery shopping", null);

        Transaction saved = createTransactionEntity(1L, user, new BigDecimal("150.00"),
                TransactionType.EXPENSE, LocalDate.of(2024, 3, 15), "Grocery shopping", null);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse response = transactionService.createTransaction(user, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(response.date()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(response.description()).isEqualTo("Grocery shopping");
        assertThat(response.categoryName()).isNull();
    }

    @Test
    void createTransaction_withCategory_success() {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("50.00"), TransactionType.EXPENSE, LocalDate.of(2024, 3, 10),
                "Lunch", 10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        Transaction saved = createTransactionEntity(2L, user, new BigDecimal("50.00"),
                TransactionType.EXPENSE, LocalDate.of(2024, 3, 10), "Lunch", category);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse response = transactionService.createTransaction(user, request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.categoryName()).isEqualTo("Food");
    }

    @Test
    void createTransaction_categoryNotFound_throwsException() {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("50.00"), TransactionType.EXPENSE, LocalDate.of(2024, 3, 10),
                "Lunch", 99L);

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(user, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateTransaction_ownedByUser_success() {
        Transaction existing = createTransactionEntity(1L, user, new BigDecimal("100.00"),
                TransactionType.EXPENSE, LocalDate.of(2024, 3, 10), "Old desc", null);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"), TransactionType.INCOME, LocalDate.of(2024, 3, 12),
                "Updated desc", null);

        Transaction saved = createTransactionEntity(1L, user, new BigDecimal("200.00"),
                TransactionType.INCOME, LocalDate.of(2024, 3, 12), "Updated desc", null);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse response = transactionService.updateTransaction(user, 1L, request);

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.type()).isEqualTo(TransactionType.INCOME);
        assertThat(response.description()).isEqualTo("Updated desc");
    }

    @Test
    void updateTransaction_notOwned_throwsAccessDenied() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        Transaction existing = createTransactionEntity(1L, otherUser, new BigDecimal("100.00"),
                TransactionType.EXPENSE, LocalDate.of(2024, 3, 10), "desc", null);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"), TransactionType.INCOME, LocalDate.of(2024, 3, 12),
                "Updated", null);

        assertThatThrownBy(() -> transactionService.updateTransaction(user, 1L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateTransaction_notFound_throwsException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"), TransactionType.INCOME, LocalDate.of(2024, 3, 12),
                "Updated", null);

        assertThatThrownBy(() -> transactionService.updateTransaction(user, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTransaction_ownedByUser_success() {
        Transaction existing = createTransactionEntity(1L, user, new BigDecimal("100.00"),
                TransactionType.EXPENSE, LocalDate.of(2024, 3, 10), "desc", null);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        transactionService.deleteTransaction(user, 1L);

        verify(transactionRepository).delete(existing);
    }

    @Test
    void deleteTransaction_notOwned_throwsAccessDenied() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        Transaction existing = createTransactionEntity(1L, otherUser, new BigDecimal("100.00"),
                TransactionType.EXPENSE, LocalDate.of(2024, 3, 10), "desc", null);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transactionService.deleteTransaction(user, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteTransaction_notFound_throwsException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTransactions_returnsPaginatedResults() {
        Transaction t1 = createTransactionEntity(1L, user, new BigDecimal("500.00"),
                TransactionType.INCOME, LocalDate.of(2024, 3, 20), "Salary", null);
        Transaction t2 = createTransactionEntity(2L, user, new BigDecimal("50.00"),
                TransactionType.EXPENSE, LocalDate.of(2024, 3, 15), "Lunch", category);

        Page<Transaction> page = new PageImpl<>(List.of(t1, t2), PageRequest.of(0, 20), 2);
        when(transactionRepository.findByUserOrderByDateDesc(any(User.class), any()))
                .thenReturn(page);

        PagedResponse<TransactionResponse> response = transactionService.getTransactions(user, 0, 20);

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);

        assertThat(response.content().get(0).id()).isEqualTo(1L);
        assertThat(response.content().get(1).categoryName()).isEqualTo("Food");
    }

    @Test
    void getTransactions_emptyPage_returnsEmptyContent() {
        Page<Transaction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(transactionRepository.findByUserOrderByDateDesc(any(User.class), any()))
                .thenReturn(emptyPage);

        PagedResponse<TransactionResponse> response = transactionService.getTransactions(user, 0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(0);
    }

    private Transaction createTransactionEntity(Long id, User user, BigDecimal amount,
                                                TransactionType type, LocalDate date,
                                                String description, Category category) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDate(date);
        transaction.setDescription(description);
        transaction.setCategory(category);
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }
}
