package com.tracker.controller;

import com.tracker.entity.User;
import com.tracker.exception.GlobalExceptionHandler;
import com.tracker.repository.UserRepository;
import com.tracker.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExportService exportService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExportController exportController;

    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(exportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, "test@example.com", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void exportCsv_validDateRange_returnsCSV() throws Exception {
        String csvContent = "date,type,amount,category,description\n2024-01-15,EXPENSE,50.00,Food,Groceries\n";

        when(exportService.generateCsv(any(User.class),
                eq(LocalDate.of(2024, 1, 1)),
                eq(LocalDate.of(2024, 3, 31))))
                .thenReturn(csvContent);

        mockMvc.perform(get("/api/export/csv")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-03-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"transactions_export.csv\""))
                .andExpect(content().string(csvContent));
    }

    @Test
    void exportCsv_noTransactions_returnsHeaderOnly() throws Exception {
        String csvContent = "date,type,amount,category,description\n";

        when(exportService.generateCsv(any(User.class),
                eq(LocalDate.of(2024, 1, 1)),
                eq(LocalDate.of(2024, 1, 31))))
                .thenReturn(csvContent);

        mockMvc.perform(get("/api/export/csv")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"transactions_export.csv\""))
                .andExpect(content().string(csvContent));
    }

    @Test
    void exportCsv_missingStartDate_returns400() throws Exception {
        mockMvc.perform(get("/api/export/csv")
                        .param("endDate", "2024-03-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportCsv_missingEndDate_returns400() throws Exception {
        mockMvc.perform(get("/api/export/csv")
                        .param("startDate", "2024-01-01"))
                .andExpect(status().isBadRequest());
    }
}
