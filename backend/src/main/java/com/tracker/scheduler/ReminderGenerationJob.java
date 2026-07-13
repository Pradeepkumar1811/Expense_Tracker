package com.tracker.scheduler;

import com.tracker.entity.Reminder;
import com.tracker.entity.Subscription;
import com.tracker.repository.ReminderRepository;
import com.tracker.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderGenerationJob.class);

    private final SubscriptionRepository subscriptionRepository;
    private final ReminderRepository reminderRepository;

    public ReminderGenerationJob(SubscriptionRepository subscriptionRepository,
                                 ReminderRepository reminderRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.reminderRepository = reminderRepository;
    }

    @Scheduled(cron = "${reminder.generation.cron:0 0 8 * * *}")
    @Transactional
    public void generateReminders() {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);

        log.info("Running reminder generation job for subscriptions with renewal between {} and {}", today, threeDaysFromNow);

        List<Subscription> upcomingSubscriptions = subscriptionRepository.findByNextRenewalDateBetween(today, threeDaysFromNow);

        int generatedCount = 0;
        for (Subscription subscription : upcomingSubscriptions) {
            if (!reminderRepository.existsBySubscriptionIdAndRenewalDate(
                    subscription.getId(), subscription.getNextRenewalDate())) {
                Reminder reminder = new Reminder();
                reminder.setUser(subscription.getUser());
                reminder.setSubscription(subscription);
                reminder.setRenewalDate(subscription.getNextRenewalDate());
                reminder.setRead(false);
                reminder.setCreatedAt(LocalDateTime.now());
                reminderRepository.save(reminder);
                generatedCount++;
            }
        }

        log.info("Reminder generation job completed. Generated {} new reminders.", generatedCount);
    }
}
