package com.tracker.service;

import com.tracker.dto.CategoryRequest;
import com.tracker.dto.CategoryResponse;
import com.tracker.entity.Category;
import com.tracker.entity.User;
import com.tracker.exception.CategoryDuplicateException;
import com.tracker.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
    }

    @Test
    void getCategories_returnsDefaultAndUserCategories() {
        Category food = createCategory(1L, "Food", null, true);
        Category rent = createCategory(2L, "Rent", null, true);
        Category custom = createCategory(5L, "Travel", user, false);

        when(categoryRepository.findAllByDefaultTrueOrUser(user))
                .thenReturn(List.of(food, rent, custom));

        List<CategoryResponse> result = categoryService.getCategories(user);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).name()).isEqualTo("Food");
        assertThat(result.get(0).isDefault()).isTrue();
        assertThat(result.get(1).name()).isEqualTo("Rent");
        assertThat(result.get(1).isDefault()).isTrue();
        assertThat(result.get(2).name()).isEqualTo("Travel");
        assertThat(result.get(2).isDefault()).isFalse();
    }

    @Test
    void getCategories_noCustomCategories_returnsOnlyDefaults() {
        Category food = createCategory(1L, "Food", null, true);
        Category rent = createCategory(2L, "Rent", null, true);

        when(categoryRepository.findAllByDefaultTrueOrUser(user))
                .thenReturn(List.of(food, rent));

        List<CategoryResponse> result = categoryService.getCategories(user);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(CategoryResponse::isDefault);
    }

    @Test
    void createCategory_uniqueName_success() {
        CategoryRequest request = new CategoryRequest("Travel");

        when(categoryRepository.findByNameAndUserIsNull("Travel")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameAndUser("Travel", user)).thenReturn(Optional.empty());

        Category saved = createCategory(5L, "Travel", user, false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.createCategory(user, request);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Travel");
        assertThat(response.isDefault()).isFalse();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_duplicatesDefaultCategory_throwsException() {
        CategoryRequest request = new CategoryRequest("Food");

        Category defaultFood = createCategory(1L, "Food", null, true);
        when(categoryRepository.findByNameAndUserIsNull("Food")).thenReturn(Optional.of(defaultFood));

        assertThatThrownBy(() -> categoryService.createCategory(user, request))
                .isInstanceOf(CategoryDuplicateException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_duplicatesUserCategory_throwsException() {
        CategoryRequest request = new CategoryRequest("Travel");

        when(categoryRepository.findByNameAndUserIsNull("Travel")).thenReturn(Optional.empty());

        Category existingCustom = createCategory(5L, "Travel", user, false);
        when(categoryRepository.findByNameAndUser("Travel", user)).thenReturn(Optional.of(existingCustom));

        assertThatThrownBy(() -> categoryService.createCategory(user, request))
                .isInstanceOf(CategoryDuplicateException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_trimsWhitespace() {
        CategoryRequest request = new CategoryRequest("  Travel  ");

        when(categoryRepository.findByNameAndUserIsNull("Travel")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameAndUser("Travel", user)).thenReturn(Optional.empty());

        Category saved = createCategory(5L, "Travel", user, false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.createCategory(user, request);

        assertThat(response.name()).isEqualTo("Travel");
    }

    private Category createCategory(Long id, String name, User user, boolean isDefault) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setUser(user);
        category.setDefault(isDefault);
        return category;
    }
}
