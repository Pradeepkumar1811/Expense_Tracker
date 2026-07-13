package com.tracker.service;

import com.tracker.dto.CategoryRequest;
import com.tracker.dto.CategoryResponse;
import com.tracker.entity.Category;
import com.tracker.entity.User;
import com.tracker.exception.CategoryDuplicateException;
import com.tracker.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(User user) {
        List<Category> categories = categoryRepository.findAllByDefaultTrueOrUser(user);
        return categories.stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(User user, CategoryRequest request) {
        String name = request.name().trim();

        // Check if a default category with this name already exists
        if (categoryRepository.findByNameAndUserIsNull(name).isPresent()) {
            throw new CategoryDuplicateException(name, user.getId());
        }

        // Check if the user already has a custom category with this name
        if (categoryRepository.findByNameAndUser(name, user).isPresent()) {
            throw new CategoryDuplicateException(name, user.getId());
        }

        Category category = new Category();
        category.setName(name);
        category.setUser(user);
        category.setDefault(false);

        Category saved = categoryRepository.save(category);
        return toCategoryResponse(saved);
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.isDefault()
        );
    }
}
