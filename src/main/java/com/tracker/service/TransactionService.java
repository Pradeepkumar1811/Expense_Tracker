package com.tracker.service;

import com.tracker.dto.PagedResponse;
import com.tracker.dto.TransactionRequest;
import com.tracker.dto.TransactionResponse;
import com.tracker.entity.Category;
import com.tracker.entity.Transaction;
import com.tracker.entity.User;
import com.tracker.exception.AccessDeniedException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.repository.CategoryRepository;
import com.tracker.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final String DASHBOARD_CACHE_KEY_PREFIX = "dashboard:";

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CacheService cacheService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              CacheService cacheService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.cacheService = cacheService;
    }

    @Transactional
    public TransactionResponse createTransaction(User user, TransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setDate(request.date());
        transaction.setDescription(request.description());
        transaction.setCreatedAt(LocalDateTime.now());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            transaction.setCategory(category);
        }

        Transaction saved = transactionRepository.save(transaction);
        cacheService.invalidate(DASHBOARD_CACHE_KEY_PREFIX + user.getId());
        return toTransactionResponse(saved);
    }

    @Transactional
    public TransactionResponse updateTransaction(User user, Long transactionId, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException();
        }

        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setDate(request.date());
        transaction.setDescription(request.description());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            transaction.setCategory(category);
        } else {
            transaction.setCategory(null);
        }

        Transaction saved = transactionRepository.save(transaction);
        cacheService.invalidate(DASHBOARD_CACHE_KEY_PREFIX + user.getId());
        return toTransactionResponse(saved);
    }

    @Transactional
    public void deleteTransaction(User user, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException();
        }

        transactionRepository.delete(transaction);
        cacheService.invalidate(DASHBOARD_CACHE_KEY_PREFIX + user.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactions(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findByUserOrderByDateDesc(user, pageable);

        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages()
        );
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        String categoryName = transaction.getCategory() != null
                ? transaction.getCategory().getName()
                : null;

        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDate(),
                transaction.getDescription(),
                categoryName,
                transaction.getCreatedAt()
        );
    }
}
