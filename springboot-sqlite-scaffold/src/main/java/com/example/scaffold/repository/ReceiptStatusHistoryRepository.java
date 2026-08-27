package com.example.scaffold.repository;

import com.example.scaffold.domain.Audits.ReceiptStatusHistory;

import java.util.List;

public interface ReceiptStatusHistoryRepository extends IRepository<ReceiptStatusHistory, Long> {
    List<ReceiptStatusHistory> findByReceiptIdOrderByCreationDateDesc(Long receiptId);
}

