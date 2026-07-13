package com.tracker.service;

import com.tracker.dto.ReminderResponse;
import com.tracker.entity.Reminder;
import com.tracker.entity.Subscription;
import com.tracker.entity.User;
import com.tracker.enums.BillingCycle;
import com.tracker.exception.AccessDeniedException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @InjectMocks
    private ReminderService reminderService;

    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        subscription = new Subscription();
        subscription.setId(1L);
        subscription.setUser(user);
        subscription.setName("Netflix");
        subscription.setAmount(new BigDecimal("15.99"));
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setNextRenewalDate(LocalDate.of(2024, 4, 15));
    }

    @Test
    void getUnreadReminders_returnsUnreadSortedByRenewalDate() {
        Reminder r1 = createReminder(1L, user, subscription, LocalDate.of(2024, 4, 15), false);
        Reminder r2 = createReminder(2L, user, subscription, LocalDate.of(2024, 4, 18), false);

        when(reminderRepository.findByUserAndIsReadFalseOrderByRenewalDateAsc(user))
                .thenReturn(List.of(r1, r2));

        List<ReminderResponse> result = reminderService.getUnreadReminders(user);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).subscriptionName()).isEqualTo("Netflix");
        assertThat(result.get(0).renewalDate()).isEqualTo(LocalDate.of(2024, 4, 15));
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("15.99"));
        assertThat(result.get(0).read()).isFalse();
        assertThat(result.get(1).renewalDate()).isEqualTo(LocalDate.of(2024, 4, 18));
    }

    @Test
    void getUnreadReminders_noReminders_returnsEmptyList() {
        when(reminderRepository.findByUserAndIsReadFalseOrderByRenewalDateAsc(user))
                .thenReturn(List.of());

        List<ReminderResponse> result = reminderService.getUnreadReminders(user);

        assertThat(result).isEmpty();
    }

    @Test
    void markAsRead_ownedByUser_success() {
        Reminder reminder = createReminder(1L, user, subscription, LocalDate.of(2024, 4, 15), false);
        when(reminderRepository.findById(1L)).thenReturn(Optional.of(reminder));

        Reminder savedReminder = createReminder(1L, user, subscription, LocalDate.of(2024, 4, 15), true);
        when(reminderRepository.save(any(Reminder.class))).thenReturn(savedReminder);

        ReminderResponse result = reminderService.markAsRead(user, 1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.read()).isTrue();
        verify(reminderRepository).save(any(Reminder.class));
    }

    @Test
    void markAsRead_notOwned_throwsAccessDenied() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        Reminder reminder = createReminder(1L, otherUser, subscription, LocalDate.of(2024, 4, 15), false);
        when(reminderRepository.findById(1L)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.markAsRead(user, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void markAsRead_notFound_throwsResourceNotFound() {
        when(reminderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.markAsRead(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Reminder createReminder(Long id, User user, Subscription subscription,
                                    LocalDate renewalDate, boolean isRead) {
        Reminder reminder = new Reminder();
        reminder.setId(id);
        reminder.setUser(user);
        reminder.setSubscription(subscription);
        reminder.setRenewalDate(renewalDate);
        reminder.setRead(isRead);
        reminder.setCreatedAt(LocalDateTime.now());
        return reminder;
    }
}
