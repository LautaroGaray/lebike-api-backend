package com.example.scaffold.repository;

import com.example.scaffold.domain.documents.Receipt;

import java.util.List;

public interface ReceiptRepository extends IRepository<Receipt, Long> {
	List<Receipt> findAllOrderByIdDesc();

	List<Receipt> findByUserAndWarehouseOrderByIdDesc(Long userId, String warehouseCode);

	List<Receipt> findByDestinyInOrderByIdDesc(List<String> warehouseCodes);

	List<Receipt> findByOriginInAndDestinyInOrderByIdDesc(List<String> originWarehouseCodes, List<String> destinyWarehouseCodes);

	boolean existsByArticleIdAndStatusLessThan(Long articleId, Integer status);
}
