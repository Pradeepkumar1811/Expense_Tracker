package com.tracker.service;

import com.tracker.dto.ReminderResponse;
import com.tracker.entity.Reminder;
import com.tracker.entity.User;
import com.tracker.exception.AccessDeniedException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.ReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> getUnreadReminders(User user) {
        List<Reminder> reminders = reminderRepository.findByUserAndIsReadFalseOrderByRenewalDateAsc(user);
        return reminders.stream()
                .map(this::toReminderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReminderResponse markAsRead(User user, Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", reminderId));

        if (!reminder.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException();
        }

        reminder.setRead(true);
        Reminder saved = reminderRepository.save(reminder);
        return toReminderResponse(saved);
    }

    private ReminderResponse toReminderResponse(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getSubscription().getName(),
                reminder.getRenewalDate(),
                reminder.getSubscription().getAmount(),
                reminder.isRead(),
                reminder.getCreatedAt()
        );
    }
}
