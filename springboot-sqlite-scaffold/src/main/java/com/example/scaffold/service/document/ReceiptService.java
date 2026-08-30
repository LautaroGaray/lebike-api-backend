package com.example.scaffold.service.document;

import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.domain.auths.Role;
import com.example.scaffold.domain.documents.DocumentsEnum;
import com.example.scaffold.domain.documents.Receipt;
import com.example.scaffold.domain.documents.ReceiptDetail;
import com.example.scaffold.domain.documents.ReceiptStatusLog;
import com.example.scaffold.domain.inventory.Article;
import com.example.scaffold.domain.inventory.Warehouse;
import com.example.scaffold.dto.document.ReceiptCreateRequestDTO;
import com.example.scaffold.dto.document.ReceiptDetailCreateRequestDTO;
import com.example.scaffold.dto.document.ReceiptDetailResponseDTO;
import com.example.scaffold.dto.document.ReceiptResponseDTO;
import com.example.scaffold.dto.document.ReceiptStatusLogResponseDTO;
import com.example.scaffold.dto.document.ReceiptUpdateRequestDTO;
import com.example.scaffold.repository.ArticleRepository;
import com.example.scaffold.repository.ReceiptRepository;
import com.example.scaffold.repository.ReceiptStatusLogRepository;
import com.example.scaffold.repository.StatusRepository;
import com.example.scaffold.repository.UserRepository;
import com.example.scaffold.repository.WarehouseRepository;
import com.example.scaffold.util.KeyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ReceiptService {

	private final ReceiptRepository receiptRepository;
	private final UserRepository userRepository;
	private final ArticleRepository articleRepository;
	private final ReceiptStatusLogRepository receiptStatusLogRepository;
	private final StatusRepository statusRepository;
	private final WarehouseRepository warehouseRepository;
	private final KeyService keyService;

	public static final int DELETED_STATUS = 0;
	public static final String RECEIPT_CREATED_EVENT = "Receipt created";
	public static final String RECEIPT_DELETED_EVENT = "Receipt deleted";

	public ReceiptService(ReceiptRepository receiptRepository,
						  UserRepository userRepository,
						  ArticleRepository articleRepository,
						  ReceiptStatusLogRepository receiptStatusLogRepository,
						  StatusRepository statusRepository,
						  WarehouseRepository warehouseRepository,
						  KeyService keyService) {
		this.receiptRepository = receiptRepository;
		this.userRepository = userRepository;
		this.articleRepository = articleRepository;
		this.receiptStatusLogRepository = receiptStatusLogRepository;
		this.statusRepository = statusRepository;
		this.warehouseRepository = warehouseRepository;
		this.keyService = keyService;
	}

	public ReceiptResponseDTO createReceiptWithDetails(ReceiptCreateRequestDTO request) {
		Users user = userRepository.findById(request.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
		String originCode = resolveWarehouseCode(request.getOrigin(), "origin");
		String destinyCode = resolveWarehouseCode(request.getDestiny(), "destiny");

		Receipt receipt = new Receipt();
		receipt.setReceiptKey(StringUtils.hasText(request.getReceiptKey()) ? request.getReceiptKey().trim() : keyService.getKey(DocumentsEnum.RECEIPT, null).getCompletKey());
		receipt.setStatus(request.getStatus()>0?request.getStatus():10);
		receipt.setOrigin(originCode);
		receipt.setDestiny(destinyCode);
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
		registerStatusLogSnapshot(created, null, created.getStatus(), created.getUser().getId(), created.getUser().getEmail(), RECEIPT_CREATED_EVENT);
		return toDto(created);
	}

	public ReceiptResponseDTO updateReceiptWithDetails(Long receiptId, ReceiptUpdateRequestDTO request) {
		Receipt receipt = receiptRepository.findById(receiptId)
				.orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
		Integer previousStatus = receipt.getStatus();

		receipt.setStatus(request.getStatus());
		receipt.setEditDate(LocalDateTime.now());

		// Edit now only appends new details; existing rows are preserved.
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

		Long actorUserId = requesterId != null ? requesterId : receipt.getUser().getId();
		String actorEmail = findUserEmailById(actorUserId);
		if (!StringUtils.hasText(actorEmail) && receipt.getUser() != null) {
			actorEmail = receipt.getUser().getEmail();
		}

		registerStatusLogSnapshot(receipt, receipt.getStatus(), DELETED_STATUS, actorUserId, actorEmail, RECEIPT_DELETED_EVENT);

		receiptRepository.deleteById(receiptId);
	}

	private void registerStatusLogIfChanged(Receipt receipt, Integer previousStatus, Integer newStatus) {
		if (receipt == null || previousStatus == null || newStatus == null || previousStatus.equals(newStatus)) {
			return;
		}

		Long actorUserId = receipt.getUser() != null ? receipt.getUser().getId() : null;
		String actorEmail = receipt.getUser() != null ? receipt.getUser().getEmail() : null;
		registerStatusLogSnapshot(receipt, previousStatus, newStatus, actorUserId, actorEmail, receipt.getDescription());
	}

	private void registerStatusLogSnapshot(Receipt receipt,
										 Integer previousStatus,
										 Integer newStatus,
										 Long actorUserId,
										 String actorEmail,
										 String eventDescription) {
		if (receipt == null || newStatus == null || actorUserId == null) {
			return;
		}

		ReceiptStatusLog statusLog = new ReceiptStatusLog();
		statusLog.setReceiptKey(receipt.getReceiptKey());
		statusLog.setReceiptId(receipt.getId());
		statusLog.setPreviousStatus(previousStatus);
		statusLog.setNewStatus(newStatus);
		statusLog.setOrigin(receipt.getOrigin());
		statusLog.setDestiny(receipt.getDestiny());
		statusLog.setDescription(eventDescription);
		statusLog.setReceiptCreationDate(receipt.getCreationDate());
		statusLog.setReceiptEditDate(receipt.getEditDate());
		statusLog.setChangedAt(LocalDateTime.now());

		statusLog.setUserId(actorUserId);
		statusLog.setUserEmail(actorEmail);

		receiptStatusLogRepository.save(statusLog);
	}

	private String findUserEmailById(Long userId) {
		if (userId == null) {
			return null;
		}
		return userRepository.findById(userId)
				.map(Users::getEmail)
				.orElse(null);
	}

	@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
	public List<ReceiptResponseDTO> findByUserAndWarehouse(Long userId, String warehouseCode) {
		String normalizedWarehouseCode = normalizeWarehouseCode(warehouseCode);
		List<Receipt> receipts = receiptRepository.findByUserAndWarehouseOrderByIdDesc(userId, normalizedWarehouseCode);
		return mapToDtoList(receipts);
	}

	@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
	public List<ReceiptResponseDTO> findAll(Long requesterUserId) {
		Users requester = userRepository.findById(requesterUserId)
				.orElseThrow(() -> new IllegalArgumentException("Requester user not found"));
		String requesterRole = normalizeRoleName(requester.getRole() != null ? requester.getRole().getName() : null);

		if (Role.OWNER.equals(requesterRole)) {
			return mapToDtoList(receiptRepository.findAllOrderByIdDesc());
		}

		List<String> allowedWarehouseCodes = toAllowedWarehouseCodes(requester);
		if (allowedWarehouseCodes.isEmpty()) {
			return Collections.emptyList();
		}

		if (Role.USER.equals(requesterRole)) {
			return mapToDtoList(receiptRepository.findByDestinyInOrderByIdDesc(allowedWarehouseCodes));
		}

		if (Role.ADMIN.equals(requesterRole)) {
			return mapToDtoList(receiptRepository.findByOriginInAndDestinyInOrderByIdDesc(allowedWarehouseCodes, allowedWarehouseCodes));
		}

		return Collections.emptyList();
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

	private String resolveWarehouseCode(String rawCode, String fieldName) {
		String normalized = normalizeWarehouseCode(rawCode);
		if (!StringUtils.hasText(normalized)) {
			throw new IllegalArgumentException("Warehouse code is required for " + fieldName);
		}
		return warehouseRepository.findByCode(normalized)
				.map(warehouse -> warehouse.getCode())
				.orElseThrow(() -> new IllegalArgumentException("Warehouse not found for " + fieldName + ": " + normalized));
	}

	private String normalizeWarehouseCode(String rawCode) {
		if (!StringUtils.hasText(rawCode)) {
			return rawCode;
		}
		String normalized = rawCode.trim();
		if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
			normalized = normalized.substring(1, normalized.length() - 1);
		}
		normalized = normalized.replace('_', '-').toUpperCase();
		return normalized;
	}

	private List<String> toAllowedWarehouseCodes(Users user) {
		if (user == null || user.getWarehousesAllowed() == null) {
			return Collections.emptyList();
		}
		List<String> codes = new ArrayList<>();
		for (Warehouse warehouse : user.getWarehousesAllowed()) {
			if (warehouse == null || !StringUtils.hasText(warehouse.getCode())) {
				continue;
			}
			codes.add(warehouse.getCode().trim().toUpperCase(Locale.ROOT));
		}
		return codes;
	}

	private String normalizeRoleName(String roleName) {
		return StringUtils.hasText(roleName) ? roleName.trim().toUpperCase(Locale.ROOT) : "";
	}
}
