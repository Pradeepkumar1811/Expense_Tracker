package com.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tracker.dto.PagedResponse;
import com.tracker.dto.TransactionRequest;
import com.tracker.dto.TransactionResponse;
import com.tracker.entity.User;
import com.tracker.enums.TransactionType;
import com.tracker.exception.AccessDeniedException;
import com.tracker.exception.GlobalExceptionHandler;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.UserRepository;
import com.tracker.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionController transactionController;

    private User user;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        // Set up SecurityContext with the authenticated user
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, "test@example.com", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void createTransaction_validRequest_returns201() throws Exception {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("100.00"), TransactionType.EXPENSE, LocalDate.of(2024, 3, 15),
                "Test expense", null);

        TransactionResponse response = new TransactionResponse(
                1L, new BigDecimal("100.00"), TransactionType.EXPENSE, LocalDate.of(2024, 3, 15),
                "Test expense", null, LocalDateTime.now());

        when(transactionService.createTransaction(any(User.class), any(TransactionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.description").value("Test expense"));
    }

    @Test
    void createTransaction_missingAmount_returns400() throws Exception {
        String json = """
                {"type": "EXPENSE", "date": "2024-03-15", "description": "No amount"}
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_missingType_returns400() throws Exception {
        String json = """
                {"amount": 100.00, "date": "2024-03-15", "description": "No type"}
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_missingDate_returns400() throws Exception {
        String json = """
                {"amount": 100.00, "type": "EXPENSE", "description": "No date"}
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTransaction_validRequest_returns200() throws Exception {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"), TransactionType.INCOME, LocalDate.of(2024, 3, 20),
                "Updated", null);

        TransactionResponse response = new TransactionResponse(
                1L, new BigDecimal("200.00"), TransactionType.INCOME, LocalDate.of(2024, 3, 20),
                "Updated", null, LocalDateTime.now());

        when(transactionService.updateTransaction(any(User.class), eq(1L), any(TransactionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void updateTransaction_notOwned_returns403() throws Exception {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"), TransactionType.INCOME, LocalDate.of(2024, 3, 20),
                "Updated", null);

        when(transactionService.updateTransaction(any(User.class), eq(1L), any(TransactionRequest.class)))
                .thenThrow(new AccessDeniedException());

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTransaction_notFound_returns404() throws Exception {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"), TransactionType.INCOME, LocalDate.of(2024, 3, 20),
                "Updated", null);

        when(transactionService.updateTransaction(any(User.class), eq(99L), any(TransactionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Transaction", 99L));

        mockMvc.perform(put("/api/transactions/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTransaction_ownedByUser_returns204() throws Exception {
        doNothing().when(transactionService).deleteTransaction(any(User.class), eq(1L));

        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTransaction_notOwned_returns403() throws Exception {
        doThrow(new AccessDeniedException()).when(transactionService).deleteTransaction(any(User.class), eq(1L));

        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactions_defaultPagination_returns200() throws Exception {
        TransactionResponse t1 = new TransactionResponse(
                1L, new BigDecimal("500.00"), TransactionType.INCOME, LocalDate.of(2024, 3, 20),
                "Salary", null, LocalDateTime.now());
        TransactionResponse t2 = new TransactionResponse(
                2L, new BigDecimal("50.00"), TransactionType.EXPENSE, LocalDate.of(2024, 3, 15),
                "Lunch", "Food", LocalDateTime.now());

        PagedResponse<TransactionResponse> pagedResponse = new PagedResponse<>(
                List.of(t1, t2), 0, 20, 2, 1);

        when(transactionService.getTransactions(any(User.class), eq(0), eq(20)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getTransactions_customPagination_returns200() throws Exception {
        PagedResponse<TransactionResponse> pagedResponse = new PagedResponse<>(
                List.of(), 2, 10, 25, 3);

        when(transactionService.getTransactions(any(User.class), eq(2), eq(10)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/transactions")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));
    }
}
