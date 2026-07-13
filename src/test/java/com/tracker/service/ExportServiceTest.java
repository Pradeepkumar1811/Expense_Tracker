package com.tracker.service;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ExportService exportService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
    }

    @Test
    void generateCsv_withTransactions_returnsCorrectCsv() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);

        Category food = new Category();
        food.setId(1L);
        food.setName("Food");

        Transaction t1 = new Transaction();
        t1.setDate(LocalDate.of(2024, 1, 15));
        t1.setType(TransactionType.EXPENSE);
        t1.setAmount(new BigDecimal("50.00"));
        t1.setCategory(food);
        t1.setDescription("Groceries");

        Transaction t2 = new Transaction();
        t2.setDate(LocalDate.of(2024, 2, 1));
        t2.setType(TransactionType.INCOME);
        t2.setAmount(new BigDecimal("3000.00"));
        t2.setCategory(null);
        t2.setDescription("Salary");

        when(transactionRepository.findByUserAndDateBetween(user, startDate, endDate))
                .thenReturn(List.of(t1, t2));

        String csv = exportService.generateCsv(user, startDate, endDate);

        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(3);
        assertThat(lines[0]).isEqualTo("date,type,amount,category,description");
        assertThat(lines[1]).isEqualTo("2024-01-15,EXPENSE,50.00,Food,Groceries");
        assertThat(lines[2]).isEqualTo("2024-02-01,INCOME,3000.00,,Salary");
    }

    @Test
    void generateCsv_noTransactions_returnsHeaderOnly() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 31);

        when(transactionRepository.findByUserAndDateBetween(user, startDate, endDate))
                .thenReturn(List.of());

        String csv = exportService.generateCsv(user, startDate, endDate);

        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(1);
        assertThat(lines[0]).isEqualTo("date,type,amount,category,description");
    }

    @Test
    void generateCsv_withNullDescription_returnsEmptyField() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        Transaction t = new Transaction();
        t.setDate(LocalDate.of(2024, 1, 10));
        t.setType(TransactionType.EXPENSE);
        t.setAmount(new BigDecimal("25.50"));
        t.setCategory(null);
        t.setDescription(null);

        when(transactionRepository.findByUserAndDateBetween(user, startDate, endDate))
                .thenReturn(List.of(t));

        String csv = exportService.generateCsv(user, startDate, endDate);

        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[1]).isEqualTo("2024-01-10,EXPENSE,25.50,,");
    }

    @Test
    void generateCsv_withCommaInDescription_escapesField() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        Transaction t = new Transaction();
        t.setDate(LocalDate.of(2024, 1, 10));
        t.setType(TransactionType.EXPENSE);
        t.setAmount(new BigDecimal("100.00"));
        t.setCategory(null);
        t.setDescription("Food, drinks");

        when(transactionRepository.findByUserAndDateBetween(user, startDate, endDate))
                .thenReturn(List.of(t));

        String csv = exportService.generateCsv(user, startDate, endDate);

        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[1]).contains("\"Food, drinks\"");
    }

    @Test
    void generateCsv_withQuoteInDescription_escapesField() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        Transaction t = new Transaction();
        t.setDate(LocalDate.of(2024, 1, 10));
        t.setType(TransactionType.EXPENSE);
        t.setAmount(new BigDecimal("100.00"));
        t.setCategory(null);
        t.setDescription("He said \"hello\"");

        when(transactionRepository.findByUserAndDateBetween(user, startDate, endDate))
                .thenReturn(List.of(t));

        String csv = exportService.generateCsv(user, startDate, endDate);

        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[1]).contains("\"He said \"\"hello\"\"\"");
    }
}
