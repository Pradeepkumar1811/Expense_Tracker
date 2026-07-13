package com.tracker.repository;

import com.tracker.entity.Budget;
import com.tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserAndMonthAndYear(User user, Integer month, Integer year);

    Optional<Budget> findByUserAndMonthAndYearAndCategoryId(User user, Integer month, Integer year, Long categoryId);

    Optional<Budget> findByUserAndMonthAndYearAndCategoryIsNull(User user, Integer month, Integer year);
}
