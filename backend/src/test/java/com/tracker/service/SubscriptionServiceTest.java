package com.tracker.service;

import com.tracker.dto.SubscriptionRequest;
import com.tracker.dto.SubscriptionResponse;
import com.tracker.entity.Subscription;
import com.tracker.entity.User;
import com.tracker.enums.BillingCycle;
import com.tracker.exception.AccessDeniedException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.SubscriptionRepository;
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
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
    }

    @Test
    void createSubscription_success() {
        SubscriptionRequest request = new SubscriptionRequest(
                "Netflix", new BigDecimal("15.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 4, 15));

        Subscription saved = createSubscriptionEntity(1L, user, "Netflix",
                new BigDecimal("15.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 4, 15));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);

        SubscriptionResponse response = subscriptionService.createSubscription(user, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Netflix");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("15.99"));
        assertThat(response.billingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(response.nextRenewalDate()).isEqualTo(LocalDate.of(2024, 4, 15));
    }

    @Test
    void createSubscription_annualBillingCycle_success() {
        SubscriptionRequest request = new SubscriptionRequest(
                "AWS", new BigDecimal("120.00"), BillingCycle.ANNUAL, LocalDate.of(2025, 1, 1));

        Subscription saved = createSubscriptionEntity(2L, user, "AWS",
                new BigDecimal("120.00"), BillingCycle.ANNUAL, LocalDate.of(2025, 1, 1));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);

        SubscriptionResponse response = subscriptionService.createSubscription(user, request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.billingCycle()).isEqualTo(BillingCycle.ANNUAL);
    }

    @Test
    void updateSubscription_ownedByUser_success() {
        Subscription existing = createSubscriptionEntity(1L, user, "Netflix",
                new BigDecimal("15.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 4, 15));
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(existing));

        SubscriptionRequest request = new SubscriptionRequest(
                "Netflix Premium", new BigDecimal("22.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 5, 15));

        Subscription saved = createSubscriptionEntity(1L, user, "Netflix Premium",
                new BigDecimal("22.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 5, 15));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);

        SubscriptionResponse response = subscriptionService.updateSubscription(user, 1L, request);

        assertThat(response.name()).isEqualTo("Netflix Premium");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("22.99"));
        assertThat(response.nextRenewalDate()).isEqualTo(LocalDate.of(2024, 5, 15));
    }

    @Test
    void updateSubscription_notOwned_throwsAccessDenied() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        Subscription existing = createSubscriptionEntity(1L, otherUser, "Netflix",
                new BigDecimal("15.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 4, 15));
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(existing));

        SubscriptionRequest request = new SubscriptionRequest(
                "Netflix Premium", new BigDecimal("22.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 5, 15));

        assertThatThrownBy(() -> subscriptionService.updateSubscription(user, 1L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateSubscription_notFound_throwsException() {
        when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        SubscriptionRequest request = new SubscriptionRequest(
                "Netflix", new BigDecimal("15.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 4, 15));

        assertThatThrownBy(() -> subscriptionService.updateSubscription(user, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteSubscription_ownedByUser_success() {
        Subscription existing = createSubscriptionEntity(1L, user, "Netflix",
                new BigDecimal("15.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 4, 15));
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(existing));

        subscriptionService.deleteSubscription(user, 1L);

        verify(subscriptionRepository).delete(existing);
    }

    @Test
    void deleteSubscription_notOwned_throwsAccessDenied() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        Subscription existing = createSubscriptionEntity(1L, otherUser, "Netflix",
                new BigDecimal("15.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 4, 15));
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> subscriptionService.deleteSubscription(user, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteSubscription_notFound_throwsException() {
        when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.deleteSubscription(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSubscriptions_returnsAllUserSubscriptions() {
        Subscription s1 = createSubscriptionEntity(1L, user, "Netflix",
                new BigDecimal("15.99"), BillingCycle.MONTHLY, LocalDate.of(2024, 4, 15));
        Subscription s2 = createSubscriptionEntity(2L, user, "AWS",
                new BigDecimal("120.00"), BillingCycle.ANNUAL, LocalDate.of(2025, 1, 1));

        when(subscriptionRepository.findByUser(user)).thenReturn(List.of(s1, s2));

        List<SubscriptionResponse> response = subscriptionService.getSubscriptions(user);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).name()).isEqualTo("Netflix");
        assertThat(response.get(0).billingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(response.get(1).name()).isEqualTo("AWS");
        assertThat(response.get(1).billingCycle()).isEqualTo(BillingCycle.ANNUAL);
    }

    @Test
    void getSubscriptions_noSubscriptions_returnsEmptyList() {
        when(subscriptionRepository.findByUser(user)).thenReturn(List.of());

        List<SubscriptionResponse> response = subscriptionService.getSubscriptions(user);

        assertThat(response).isEmpty();
    }

    private Subscription createSubscriptionEntity(Long id, User user, String name,
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
