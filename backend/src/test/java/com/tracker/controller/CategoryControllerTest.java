package com.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.dto.CategoryRequest;
import com.tracker.dto.CategoryResponse;
import com.tracker.entity.User;
import com.tracker.exception.CategoryDuplicateException;
import com.tracker.exception.GlobalExceptionHandler;
import com.tracker.repository.UserRepository;
import com.tracker.service.CategoryService;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoryControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryController categoryController;

    private User user;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
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
    void createCategory_validRequest_returns201() throws Exception {
        CategoryRequest request = new CategoryRequest("Travel");

        CategoryResponse response = new CategoryResponse(5L, "Travel", false);
        when(categoryService.createCategory(any(User.class), any(CategoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Travel"))
                .andExpect(jsonPath("$.isDefault").value(false));
    }

    @Test
    void createCategory_blankName_returns400() throws Exception {
        String json = """
                {"name": ""}
                """;

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_nullName_returns400() throws Exception {
        String json = """
                {}
                """;

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_duplicateName_returns409() throws Exception {
        CategoryRequest request = new CategoryRequest("Food");

        when(categoryService.createCategory(any(User.class), any(CategoryRequest.class)))
                .thenThrow(new CategoryDuplicateException("Food", 1L));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void getCategories_returnsListOfCategories() throws Exception {
        List<CategoryResponse> categories = List.of(
                new CategoryResponse(1L, "Food", true),
                new CategoryResponse(2L, "Rent", true),
                new CategoryResponse(3L, "Shopping", true),
                new CategoryResponse(4L, "Fuel", true),
                new CategoryResponse(5L, "Travel", false)
        );

        when(categoryService.getCategories(any(User.class))).thenReturn(categories);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].name").value("Food"))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[4].name").value("Travel"))
                .andExpect(jsonPath("$[4].isDefault").value(false));
    }

    @Test
    void getCategories_emptyList_returnsEmptyArray() throws Exception {
        when(categoryService.getCategories(any(User.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
