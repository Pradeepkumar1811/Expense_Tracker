package com.tracker.repository;

import com.tracker.entity.Category;
import com.tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.isDefault = true OR c.user = :user")
    List<Category> findAllByDefaultTrueOrUser(@Param("user") User user);

    Optional<Category> findByNameAndUser(String name, User user);

    Optional<Category> findByNameAndUserIsNull(String name);

    List<Category> findAllByIsDefaultTrue();
}
