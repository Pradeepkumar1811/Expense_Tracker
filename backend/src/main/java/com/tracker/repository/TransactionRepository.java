package com.tracker.repository;

import com.tracker.entity.Transaction;
import com.tracker.entity.User;
import com.tracker.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserOrderByDateDesc(User user, Pageable pageable);

    List<Transaction> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user = :user AND t.type = :type " +
           "AND t.date >= :startDate AND t.date <= :endDate")
    BigDecimal sumAmountByUserAndTypeAndDateBetween(
            @Param("user") User user,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user = :user AND t.type = :type " +
           "AND t.date >= :startDate AND t.date <= :endDate " +
           "AND t.category.id = :categoryId")
    BigDecimal sumAmountByUserAndTypeAndDateBetweenAndCategoryId(
            @Param("user") User user,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId);
}
