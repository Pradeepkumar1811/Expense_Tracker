package com.tracker.repository;

import com.tracker.entity.Subscription;
import com.tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUser(User user);

    List<Subscription> findByUserAndNextRenewalDateBetween(User user, LocalDate startDate, LocalDate endDate);

    List<Subscription> findByNextRenewalDateBetween(LocalDate startDate, LocalDate endDate);
}
