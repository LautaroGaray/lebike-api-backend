package com.example.scaffold.service.document;

import com.example.scaffold.domain.Audits.ReceiptStatusHistory;
import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.domain.documents.DocumentsEnum;
import com.example.scaffold.domain.documents.Receipt;
import com.example.scaffold.domain.documents.ReceiptDetail;
import com.example.scaffold.domain.documents.ReceiptStatusLog;
import com.example.scaffold.domain.inventory.Article;
import com.example.scaffold.dto.document.ReceiptCreateRequestDTO;
import com.example.scaffold.dto.document.ReceiptDetailCreateRequestDTO;
import com.example.scaffold.dto.document.ReceiptDetailResponseDTO;
import com.example.scaffold.dto.document.ReceiptResponseDTO;
import com.example.scaffold.dto.document.ReceiptStatusLogResponseDTO;
import com.example.scaffold.repository.ArticleRepository;
import com.example.scaffold.repository.ReceiptRepository;
import com.example.scaffold.repository.ReceiptStatusHistoryRepository;
import com.example.scaffold.repository.ReceiptStatusLogRepository;
import com.example.scaffold.repository.StatusRepository;
import com.example.scaffold.repository.UserRepository;
import com.example.scaffold.util.KeyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ReceiptService {

	private final ReceiptRepository receiptRepository;
	private final UserRepository userRepository;
	private final ArticleRepository articleRepository;
	private final ReceiptStatusLogRepository receiptStatusLogRepository;
	private final ReceiptStatusHistoryRepository receiptStatusHistoryRepository;
	private final StatusRepository statusRepository;
	private final KeyService keyService;

	public static final int DELETED_STATUS = 0;

	public ReceiptService(ReceiptRepository receiptRepository,
						  UserRepository userRepository,
						  ArticleRepository articleRepository,
						  ReceiptStatusLogRepository receiptStatusLogRepository,
						  ReceiptStatusHistoryRepository receiptStatusHistoryRepository,
						  StatusRepository statusRepository,
						  KeyService keyService) {
		this.receiptRepository = receiptRepository;
		this.userRepository = userRepository;
		this.articleRepository = articleRepository;
		this.receiptStatusLogRepository = receiptStatusLogRepository;
		this.receiptStatusHistoryRepository = receiptStatusHistoryRepository;
		this.statusRepository = statusRepository;
		this.keyService = keyService;
	}

	public ReceiptResponseDTO createReceiptWithDetails(ReceiptCreateRequestDTO request) {
		Users user = userRepository.findById(request.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));

		Receipt receipt = new Receipt();
		receipt.setReceiptKey(StringUtils.hasText(request.getReceiptKey()) ? request.getReceiptKey().trim() : keyService.getKey(DocumentsEnum.RECEIPT, null).getCompletKey());
		receipt.setStatus(request.getStatus());
		receipt.setOrigin(request.getOrigin().trim());
		receipt.setDestiny(request.getDestiny().trim());
		receipt.setDescription(request.getDescription());
		receipt.setUser(user);
		receipt.setCreationDate(LocalDateTime.now());

		List<ReceiptDetail> detailList = new ArrayList<>();
		for (ReceiptDetailCreateRequestDTO detailRequest : request.getDetails()) {
			if (detailRequest == null || detailRequest.getArticleId() == null) {
				continue;
			}

			Article article = articleRepository.findById(detailRequest.getArticleId())
					.orElseThrow(() -> new IllegalArgumentException("Article not found: " + detailRequest.getArticleId()));

			ReceiptDetail detail = new ReceiptDetail();
			detail.setReceipt(receipt);
			detail.setArticle(article);
			detail.setCreationDate(LocalDateTime.now());
			detailList.add(detail);
		}
		receipt.getDetaile().addAll(detailList);

		Receipt created = receiptRepository.save(receipt);
		return toDto(created);
	}

	public ReceiptResponseDTO updateReceiptWithDetails(Long receiptId, ReceiptCreateRequestDTO request) {
		Receipt receipt = receiptRepository.findById(receiptId)
				.orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
		Integer previousStatus = receipt.getStatus();

		Users user = userRepository.findById(request.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));

		receipt.setStatus(request.getStatus());
		receipt.setOrigin(request.getOrigin().trim());
		receipt.setDestiny(request.getDestiny().trim());
		receipt.setDescription(request.getDescription());
		receipt.setUser(user);
		receipt.setEditDate(LocalDateTime.now());

		receipt.getDetaile().clear();
		for (ReceiptDetailCreateRequestDTO detailRequest : request.getDetails()) {
			if (detailRequest == null || detailRequest.getArticleId() == null) {
				continue;
			}

			Article article = articleRepository.findById(detailRequest.getArticleId())
					.orElseThrow(() -> new IllegalArgumentException("Article not found: " + detailRequest.getArticleId()));

			ReceiptDetail detail = new ReceiptDetail();
			detail.setReceipt(receipt);
			detail.setArticle(article);
			detail.setCreationDate(LocalDateTime.now());
			receipt.getDetaile().add(detail);
		}

		Receipt updated = receiptRepository.save(receipt);
		registerStatusLogIfChanged(updated, previousStatus, updated.getStatus());
		return toDto(updated);
	}

	public void deleteReceipt(Long receiptId, Long requesterId) {
		Receipt receipt = receiptRepository.findById(receiptId)
				.orElseThrow(() -> new IllegalArgumentException("Receipt not found"));

		// Log deletion in ReceiptStatusHistory before removing the record
		ReceiptStatusHistory history = new ReceiptStatusHistory();
		history.setReceiptId(receipt.getId());
		history.setReceiptKey(receipt.getReceiptKey());
		history.setStatus(DELETED_STATUS);
		history.setDescription("Receipt deleted");
		history.setUserId(requesterId);
		if (receipt.getUser() != null) {
			history.setUserEmail(receipt.getUser().getEmail());
		}
		history.setCreationDate(LocalDateTime.now());
		receiptStatusHistoryRepository.save(history);

		receiptRepository.deleteById(receiptId);
	}

	private void registerStatusLogIfChanged(Receipt receipt, Integer previousStatus, Integer newStatus) {
		if (receipt == null || previousStatus == null || newStatus == null || previousStatus.equals(newStatus)) {
			return;
		}

		ReceiptStatusLog statusLog = new ReceiptStatusLog();
		statusLog.setReceiptKey(receipt.getReceiptKey());
		statusLog.setReceiptId(receipt.getId());
		statusLog.setPreviousStatus(previousStatus);
		statusLog.setNewStatus(newStatus);
		statusLog.setOrigin(receipt.getOrigin());
		statusLog.setDestiny(receipt.getDestiny());
		statusLog.setDescription(receipt.getDescription());
		statusLog.setReceiptCreationDate(receipt.getCreationDate());
		statusLog.setReceiptEditDate(receipt.getEditDate());
		statusLog.setChangedAt(LocalDateTime.now());

		if (receipt.getUser() != null) {
			statusLog.setUserId(receipt.getUser().getId());
			statusLog.setUserEmail(receipt.getUser().getEmail());
		}

		receiptStatusLogRepository.save(statusLog);
	}

	@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
	public List<ReceiptResponseDTO> findByUserAndWarehouse(Long userId, String warehouseCode) {
		List<Receipt> receipts = receiptRepository.findByUserAndWarehouseOrderByIdDesc(userId, warehouseCode);
		return mapToDtoList(receipts);
	}

	@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
	public List<ReceiptResponseDTO> findAll() {
		List<Receipt> receipts = receiptRepository.findAllOrderByIdDesc();
		return mapToDtoList(receipts);
	}

	@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
	public List<ReceiptStatusLogResponseDTO> findStatusHistory(Long receiptId) {
		List<ReceiptStatusLog> logs = receiptStatusLogRepository.findByReceiptIdOrderByChangedAtDesc(receiptId);
		List<ReceiptStatusLogResponseDTO> response = new ArrayList<>();
		for (ReceiptStatusLog log : logs) {
			if (log == null) {
				continue;
			}
			response.add(toHistoryDto(log));
		}
		return response;
	}


	private List<ReceiptResponseDTO> mapToDtoList(List<Receipt> receipts) {
		List<ReceiptResponseDTO> response = new ArrayList<>();
		for (Receipt receipt : receipts) {
			response.add(toDto(receipt));
		}
		return response;
	}

	private ReceiptResponseDTO toDto(Receipt receipt) {
		ReceiptResponseDTO dto = new ReceiptResponseDTO();
		dto.setId(receipt.getId());
		dto.setStatus(receipt.getStatus());
		dto.setReceiptKey(receipt.getReceiptKey());
		dto.setStatusDescription(statusRepository.findByStatus(receipt.getStatus())
				.map(status -> status.getDescription())
				.orElse(null));
		dto.setOrigin(receipt.getOrigin());
		dto.setDestiny(receipt.getDestiny());
		dto.setDescription(receipt.getDescription());
		dto.setCreationDate(receipt.getCreationDate());
		dto.setEditDate(receipt.getEditDate());

		if (receipt.getUser() != null) {
			dto.setUserId(receipt.getUser().getId());
			dto.setUsername(receipt.getUser().getUsername());
			dto.setUserEmail(receipt.getUser().getEmail());
		}

		if (receipt.getDetaile() != null) {
			for (ReceiptDetail detail : receipt.getDetaile()) {
				if (detail == null) {
					continue;
				}
				ReceiptDetailResponseDTO detailDTO = new ReceiptDetailResponseDTO();
				detailDTO.setId(detail.getId());
				detailDTO.setCreationDate(detail.getCreationDate());
				detailDTO.setEditDate(detail.getEditDate());
				if (!Objects.isNull(detail.getArticle())) {
					detailDTO.setArticleId(detail.getArticle().getId());
					detailDTO.setArticleSku(detail.getArticle().getSku());
					detailDTO.setArticleName(detail.getArticle().getName());
				}
				dto.getDetails().add(detailDTO);
			}
		}

		return dto;
	}

	private ReceiptStatusLogResponseDTO toHistoryDto(ReceiptStatusLog log) {
		ReceiptStatusLogResponseDTO dto = new ReceiptStatusLogResponseDTO();
		dto.setId(log.getId());
		dto.setReceiptId(log.getReceiptId());
		dto.setReceiptKey(log.getReceiptKey());
		dto.setPreviousStatus(log.getPreviousStatus());
		dto.setPreviousStatusDescription(log.getPreviousStatus() == null
				? null
				: statusRepository.findByStatus(log.getPreviousStatus())
				.map(status -> status.getDescription())
				.orElse(null));
		dto.setNewStatus(log.getNewStatus());
		dto.setNewStatusDescription(log.getNewStatus() == null
				? null
				: statusRepository.findByStatus(log.getNewStatus())
				.map(status -> status.getDescription())
				.orElse(null));
		dto.setOrigin(log.getOrigin());
		dto.setDestiny(log.getDestiny());
		dto.setDescription(log.getDescription());
		dto.setUserId(log.getUserId());
		dto.setUserEmail(log.getUserEmail());
		dto.setReceiptCreationDate(log.getReceiptCreationDate());
		dto.setReceiptEditDate(log.getReceiptEditDate());
		dto.setChangedAt(log.getChangedAt());
		return dto;
	}
}
