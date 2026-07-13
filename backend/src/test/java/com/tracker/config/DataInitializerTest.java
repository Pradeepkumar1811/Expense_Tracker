package com.tracker.config;

import com.tracker.entity.Category;
import com.tracker.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Captor
    private ArgumentCaptor<Category> categoryCaptor;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(categoryRepository);
    }

    @Test
    void initDefaultCategories_createsAllFourDefaultCategories_whenNoneExist() {
        when(categoryRepository.findByNameAndUserIsNull(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findAllByIsDefaultTrue()).thenReturn(List.of());

        dataInitializer.initDefaultCategories();

        verify(categoryRepository, times(4)).save(categoryCaptor.capture());
        List<Category> savedCategories = categoryCaptor.getAllValues();

        assertThat(savedCategories).hasSize(4);
        assertThat(savedCategories).extracting(Category::getName)
                .containsExactly("Food", "Rent", "Shopping", "Fuel");
        assertThat(savedCategories).allMatch(Category::isDefault);
        assertThat(savedCategories).allMatch(c -> c.getUser() == null);
    }

    @Test
    void initDefaultCategories_skipsExistingCategories_whenSomeAlreadyExist() {
        Category existingFood = new Category();
        existingFood.setName("Food");
        existingFood.setDefault(true);

        when(categoryRepository.findByNameAndUserIsNull("Food")).thenReturn(Optional.of(existingFood));
        when(categoryRepository.findByNameAndUserIsNull("Rent")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameAndUserIsNull("Shopping")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameAndUserIsNull("Fuel")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findAllByIsDefaultTrue()).thenReturn(List.of());

        dataInitializer.initDefaultCategories();

        verify(categoryRepository, times(3)).save(categoryCaptor.capture());
        List<Category> savedCategories = categoryCaptor.getAllValues();

        assertThat(savedCategories).extracting(Category::getName)
                .containsExactly("Rent", "Shopping", "Fuel");
        assertThat(savedCategories).noneMatch(c -> "Food".equals(c.getName()));
    }

    @Test
    void initDefaultCategories_createsNothing_whenAllAlreadyExist() {
        when(categoryRepository.findByNameAndUserIsNull(anyString())).thenReturn(Optional.of(new Category()));
        when(categoryRepository.findAllByIsDefaultTrue()).thenReturn(List.of());

        dataInitializer.initDefaultCategories();

        verify(categoryRepository, never()).save(any(Category.class));
    }
}
