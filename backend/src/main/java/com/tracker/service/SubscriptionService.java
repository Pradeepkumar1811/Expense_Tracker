package com.tracker.service;

import com.tracker.dto.SubscriptionRequest;
import com.tracker.dto.SubscriptionResponse;
import com.tracker.entity.Subscription;
import com.tracker.entity.User;
import com.tracker.exception.AccessDeniedException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private static final String DASHBOARD_CACHE_KEY_PREFIX = "dashboard:";

    private final SubscriptionRepository subscriptionRepository;
    private final CacheService cacheService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               CacheService cacheService) {
        this.subscriptionRepository = subscriptionRepository;
        this.cacheService = cacheService;
    }

    @Transactional
    public SubscriptionResponse createSubscription(User user, SubscriptionRequest request) {
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setName(request.name());
        subscription.setAmount(request.amount());
        subscription.setBillingCycle(request.billingCycle());
        subscription.setNextRenewalDate(request.nextRenewalDate());
        subscription.setCreatedAt(LocalDateTime.now());

        Subscription saved = subscriptionRepository.save(subscription);
        cacheService.invalidate(DASHBOARD_CACHE_KEY_PREFIX + user.getId());
        return toSubscriptionResponse(saved);
    }

    @Transactional
    public SubscriptionResponse updateSubscription(User user, Long subscriptionId, SubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));

        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException();
        }

        subscription.setName(request.name());
        subscription.setAmount(request.amount());
        subscription.setBillingCycle(request.billingCycle());
        subscription.setNextRenewalDate(request.nextRenewalDate());

        Subscription saved = subscriptionRepository.save(subscription);
        cacheService.invalidate(DASHBOARD_CACHE_KEY_PREFIX + user.getId());
        return toSubscriptionResponse(saved);
    }

    @Transactional
    public void deleteSubscription(User user, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));

        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException();
        }

        subscriptionRepository.delete(subscription);
        cacheService.invalidate(DASHBOARD_CACHE_KEY_PREFIX + user.getId());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptions(User user) {
        List<Subscription> subscriptions = subscriptionRepository.findByUser(user);
        return subscriptions.stream()
                .map(this::toSubscriptionResponse)
                .collect(Collectors.toList());
    }

    private SubscriptionResponse toSubscriptionResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getName(),
                subscription.getAmount(),
                subscription.getBillingCycle(),
                subscription.getNextRenewalDate()
        );
    }
}
