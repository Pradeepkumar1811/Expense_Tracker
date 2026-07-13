package com.tracker.scheduler;

import com.tracker.entity.Reminder;
import com.tracker.entity.Subscription;
import com.tracker.entity.User;
import com.tracker.enums.BillingCycle;
import com.tracker.repository.ReminderRepository;
import com.tracker.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderGenerationJobTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ReminderRepository reminderRepository;

    @InjectMocks
    private ReminderGenerationJob reminderGenerationJob;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
    }

    @Test
    void generateReminders_createsReminderForUpcomingSubscription() {
        LocalDate renewalDate = LocalDate.now().plusDays(2);
        Subscription subscription = createSubscription(1L, user, "Netflix",
                new BigDecimal("15.99"), BillingCycle.MONTHLY, renewalDate);

        when(subscriptionRepository.findByNextRenewalDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(subscription));
        when(reminderRepository.existsBySubscriptionIdAndRenewalDate(1L, renewalDate))
                .thenReturn(false);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reminderGenerationJob.generateReminders();

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(captor.capture());

        Reminder saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getSubscription()).isEqualTo(subscription);
        assertThat(saved.getRenewalDate()).isEqualTo(renewalDate);
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void generateReminders_skipsDuplicateReminder() {
        LocalDate renewalDate = LocalDate.now().plusDays(1);
        Subscription subscription = createSubscription(1L, user, "Netflix",
                new BigDecimal("15.99"), BillingCycle.MONTHLY, renewalDate);

        when(subscriptionRepository.findByNextRenewalDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(subscription));
        when(reminderRepository.existsBySubscriptionIdAndRenewalDate(1L, renewalDate))
                .thenReturn(true);

        reminderGenerationJob.generateReminders();

        verify(reminderRepository, never()).save(any(Reminder.class));
    }

    @Test
    void generateReminders_handlesMultipleSubscriptions() {
        LocalDate renewalDate1 = LocalDate.now().plusDays(1);
        LocalDate renewalDate2 = LocalDate.now().plusDays(3);

        Subscription s1 = createSubscription(1L, user, "Netflix",
                new BigDecimal("15.99"), BillingCycle.MONTHLY, renewalDate1);
        Subscription s2 = createSubscription(2L, user, "Spotify",
                new BigDecimal("9.99"), BillingCycle.MONTHLY, renewalDate2);

        when(subscriptionRepository.findByNextRenewalDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(s1, s2));
        when(reminderRepository.existsBySubscriptionIdAndRenewalDate(1L, renewalDate1))
                .thenReturn(false);
        when(reminderRepository.existsBySubscriptionIdAndRenewalDate(2L, renewalDate2))
                .thenReturn(false);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reminderGenerationJob.generateReminders();

        verify(reminderRepository, times(2)).save(any(Reminder.class));
    }

    @Test
    void generateReminders_noUpcomingSubscriptions_createsNoReminders() {
        when(subscriptionRepository.findByNextRenewalDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        reminderGenerationJob.generateReminders();

        verify(reminderRepository, never()).save(any(Reminder.class));
    }

    private Subscription createSubscription(Long id, User user, String name,
                                            BigDecimal amount, BillingCycle billingCycle,
                                            LocalDate nextRenewalDate) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setUser(user);
        subscription.setName(name);
        subscription.setAmount(amount);
        subscription.setBillingCycle(billingCycle);
        subscription.setNextRenewalDate(nextRenewalDate);
        subscription.setCreatedAt(LocalDateTime.now());
        return subscription;
    }
}
