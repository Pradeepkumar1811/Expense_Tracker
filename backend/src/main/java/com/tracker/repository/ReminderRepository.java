package com.tracker.repository;

import com.tracker.entity.Reminder;
import com.tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserAndIsReadFalseOrderByRenewalDateAsc(User user);

    Optional<Reminder> findBySubscriptionIdAndRenewalDate(Long subscriptionId, LocalDate renewalDate);

    boolean existsBySubscriptionIdAndRenewalDate(Long subscriptionId, LocalDate renewalDate);
}
