package com.tracker.service;

import com.tracker.entity.Transaction;
import com.tracker.entity.User;
import com.tracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ExportService {

    private static final String CSV_HEADER = "date,type,amount,category,description";

    private final TransactionRepository transactionRepository;

    public ExportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public String generateCsv(User user, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(user, startDate, endDate);

        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER).append("\n");

        for (Transaction transaction : transactions) {
            csv.append(escapeCsvField(transaction.getDate().toString())).append(",");
            csv.append(escapeCsvField(transaction.getType().name())).append(",");
            csv.append(escapeCsvField(transaction.getAmount().toPlainString())).append(",");
            csv.append(escapeCsvField(transaction.getCategory() != null ? transaction.getCategory().getName() : "")).append(",");
            csv.append(escapeCsvField(transaction.getDescription() != null ? transaction.getDescription() : ""));
            csv.append("\n");
        }

        return csv.toString();
    }

    private String escapeCsvField(String field) {
        if (field == null || field.isEmpty()) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
