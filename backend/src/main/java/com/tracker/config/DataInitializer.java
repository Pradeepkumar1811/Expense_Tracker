package com.tracker.config;

import com.tracker.entity.Category;
import com.tracker.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Initializes default categories on application startup.
 * Idempotent: only creates categories that do not already exist.
 */
@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final List<String> DEFAULT_CATEGORY_NAMES = List.of(
            "Food", "Rent", "Shopping", "Fuel"
    );

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @PostConstruct
    @Transactional
    public void initDefaultCategories() {
        for (String name : DEFAULT_CATEGORY_NAMES) {
            if (categoryRepository.findByNameAndUserIsNull(name).isEmpty()) {
                Category category = new Category();
                category.setName(name);
                category.setUser(null);
                category.setDefault(true);
                categoryRepository.save(category);
                log.info("Created default category: {}", name);
            }
        }
        log.info("Default category initialization complete. Total default categories: {}",
                categoryRepository.findAllByIsDefaultTrue().size());
    }
}
