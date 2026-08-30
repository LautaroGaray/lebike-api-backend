package com.example.scaffold.repository;

import com.example.scaffold.domain.documents.ReceiptStatusLog;

import java.util.List;

public interface ReceiptStatusLogRepository extends IRepository<ReceiptStatusLog, Long> {
    List<ReceiptStatusLog> findByReceiptIdOrderByChangedAtDesc(Long receiptId);
}

