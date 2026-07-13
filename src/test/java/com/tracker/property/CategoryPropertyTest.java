package com.tracker.property;

import com.tracker.dto.CategoryRequest;
import com.tracker.dto.CategoryResponse;
import com.tracker.entity.Category;
import com.tracker.entity.User;
import com.tracker.exception.CategoryDuplicateException;
import com.tracker.repository.CategoryRepository;
import com.tracker.service.CategoryService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for CategoryService using jqwik.
 *
 * Feature: expense-subscription-tracker
 */
class CategoryPropertyTest {

    private static final List<String> DEFAULT_CATEGORY_NAMES = List.of("Food", "Rent", "Shopping", "Fuel");

    /**
     * Property 9: Category listing completeness
     *
     * For any user, querying their categories SHALL return all four default categories
     * (Food, Rent, Shopping, Fuel) plus all custom categories created by that user,
     * and no categories from other users.
     *
     * **Validates: Requirements 4.1, 4.4**
     */
    @Property(tries = 100)
    @Tag("Feature_expense-subscription-tracker")
    @Tag("Property_9_Category_listing_completeness")
    void categoryListingReturnsAllDefaultsPlusUserCustomsAndNoOtherUserCategories(
            @ForAll @LongRange(min = 1, max = 1000) long userId,
            @ForAll @LongRange(min = 1, max = 1000) long otherUserId,
            @ForAll("customCategoryNames") List<String> userCustomNames,
            @ForAll("customCategoryNames") List<String> otherUserCustomNames
    ) {
        // Ensure distinct users
        Assume.that(userId != otherUserId);

        // Setup
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        CategoryService categoryService = new CategoryService(categoryRepository);

        User user = new User();
        user.setId(userId);
        user.setEmail("user_" + userId + "@example.com");

        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setEmail("other_" + otherUserId + "@example.com");

        // Build default categories
        List<Category> defaultCategories = new ArrayList<>();
        long idCounter = 1L;
        for (String name : DEFAULT_CATEGORY_NAMES) {
            Category c = new Category();
            c.setId(idCounter++);
            c.setName(name);
            c.setUser(null); // default categories have null user
            c.setDefault(true);
            defaultCategories.add(c);
        }

        // Build user's custom categories
        List<Category> userCustomCategories = new ArrayList<>();
        for (String name : userCustomNames) {
            Category c = new Category();
            c.setId(idCounter++);
            c.setName(name);
            c.setUser(user);
            c.setDefault(false);
            userCustomCategories.add(c);
        }

        // Build other user's custom categories (should NOT appear in result)
        List<Category> otherUserCustomCategories = new ArrayList<>();
        for (String name : otherUserCustomNames) {
            Category c = new Category();
            c.setId(idCounter++);
            c.setName(name);
            c.setUser(otherUser);
            c.setDefault(false);
            otherUserCustomCategories.add(c);
        }

        // The repository should return defaults + user's custom categories only
        List<Category> repositoryResult = new ArrayList<>();
        repositoryResult.addAll(defaultCategories);
        repositoryResult.addAll(userCustomCategories);

        when(categoryRepository.findAllByDefaultTrueOrUser(user)).thenReturn(repositoryResult);

        // Act
        List<CategoryResponse> result = categoryService.getCategories(user);

        // Assert: all 4 default categories are present
        List<String> resultNames = result.stream()
                .map(CategoryResponse::name)
                .collect(Collectors.toList());

        for (String defaultName : DEFAULT_CATEGORY_NAMES) {
            assertThat(resultNames).contains(defaultName);
        }

        // Assert: all user custom categories are present
        for (String customName : userCustomNames) {
            assertThat(resultNames).contains(customName);
        }

        // Assert: no other user's categories are present
        // The result should only contain defaults + user's customs
        assertThat(result).hasSize(DEFAULT_CATEGORY_NAMES.size() + userCustomNames.size());

        // Assert: default categories are marked as default
        List<CategoryResponse> defaultsInResult = result.stream()
                .filter(CategoryResponse::isDefault)
                .collect(Collectors.toList());
        assertThat(defaultsInResult).hasSize(DEFAULT_CATEGORY_NAMES.size());

        // Assert: user custom categories are NOT marked as default
        List<CategoryResponse> customsInResult = result.stream()
                .filter(cr -> !cr.isDefault())
                .collect(Collectors.toList());
        assertThat(customsInResult).hasSize(userCustomNames.size());

        // Verify the repository was called with the correct user
        verify(categoryRepository).findAllByDefaultTrueOrUser(user);
    }

    /**
     * Property 10: Category uniqueness enforcement
     *
     * For any user who already has a category with a given name (whether default or custom),
     * attempting to create another category with the same name SHALL be rejected with
     * status 409 (via CategoryDuplicateException).
     *
     * **Validates: Requirements 4.5**
     */
    @Property(tries = 100)
    @Tag("Feature_expense-subscription-tracker")
    @Tag("Property_10_Category_uniqueness_enforcement")
    void duplicateCategoryNameIsRejected(
            @ForAll @LongRange(min = 1, max = 1000) long userId,
            @ForAll("existingCategoryName") String existingName,
            @ForAll("duplicateSource") DuplicateSource source
    ) {
        // Setup
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        CategoryService categoryService = new CategoryService(categoryRepository);

        User user = new User();
        user.setId(userId);
        user.setEmail("user_" + userId + "@example.com");

        // Configure mock based on the source of duplication
        if (source == DuplicateSource.DEFAULT_CATEGORY) {
            // The name exists as a default category (user is null)
            Category existingDefault = new Category();
            existingDefault.setId(1L);
            existingDefault.setName(existingName);
            existingDefault.setUser(null);
            existingDefault.setDefault(true);

            when(categoryRepository.findByNameAndUserIsNull(existingName))
                    .thenReturn(Optional.of(existingDefault));
            when(categoryRepository.findByNameAndUser(existingName, user))
                    .thenReturn(Optional.empty());
        } else {
            // The name exists as a user's custom category
            Category existingCustom = new Category();
            existingCustom.setId(2L);
            existingCustom.setName(existingName);
            existingCustom.setUser(user);
            existingCustom.setDefault(false);

            when(categoryRepository.findByNameAndUserIsNull(existingName))
                    .thenReturn(Optional.empty());
            when(categoryRepository.findByNameAndUser(existingName, user))
                    .thenReturn(Optional.of(existingCustom));
        }

        // Act & Assert: creating a category with the same name should throw CategoryDuplicateException
        CategoryRequest request = new CategoryRequest(existingName);

        assertThatThrownBy(() -> categoryService.createCategory(user, request))
                .isInstanceOf(CategoryDuplicateException.class);

        // Verify no category was saved
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // --- Arbitrary Providers ---

    @Provide
    Arbitrary<List<String>> customCategoryNames() {
        Arbitrary<String> nameArb = Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(20)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase());

        return nameArb.list()
                .ofMinSize(0)
                .ofMaxSize(5)
                .uniqueElements();
    }

    @Provide
    Arbitrary<String> existingCategoryName() {
        // Mix of default category names and custom-style names
        Arbitrary<String> defaultNames = Arbitraries.of(DEFAULT_CATEGORY_NAMES);
        Arbitrary<String> customNames = Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(20)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase());

        return Arbitraries.oneOf(defaultNames, customNames);
    }

    @Provide
    Arbitrary<DuplicateSource> duplicateSource() {
        return Arbitraries.of(DuplicateSource.values());
    }

    /**
     * Enum to represent the source of the duplicate category.
     */
    enum DuplicateSource {
        DEFAULT_CATEGORY,
        USER_CUSTOM_CATEGORY
    }
}
