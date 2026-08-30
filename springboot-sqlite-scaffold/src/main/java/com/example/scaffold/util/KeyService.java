package com.example.scaffold.util;

import com.example.scaffold.domain.Audits.Keys;
import com.example.scaffold.domain.documents.DocumentsEnum;
import com.example.scaffold.dto.inventory.KeyDTO;
import com.example.scaffold.repository.KeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class KeyService {

    private static final int MAX_NUMERIC = 9_999_999;
    private static final String MIN_LETTER = "AAAA";
    private static final String MAX_LETTER = "ZZZZ";

    private final KeyRepository keyRepository;

    public KeyService(KeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public KeyDTO getKey(DocumentsEnum entity, String key){
        if (entity == null) {
            throw new IllegalArgumentException("entity is required");
        }

        String targetDestiny = entity.getTargetDestiny();
        if (StringUtils.hasText(key) && !targetDestiny.equalsIgnoreCase(key.trim())) {
            throw new IllegalArgumentException("key must match entity target destiny");
        }

        Keys config = keyRepository.findByTargetDestinyForUpdate(targetDestiny)
                .orElseThrow(() -> new IllegalArgumentException("Key config not found for entity: " + targetDestiny));

        int oldNumber = normalizeNumber(config.getIncrementaNumberKey());
        String oldLetter = normalizeLetter(config.getIncrementalLetterKey());

        int newNumber = oldNumber;
        String newLetter = oldLetter;
        if (oldNumber >= MAX_NUMERIC) {
            newNumber = 1;
            newLetter = incrementLetter(oldLetter);
        } else {
            newNumber = oldNumber + 1;
        }

        config.setIncrementaNumberKey(newNumber);
        config.setIncrementalLetterKey(newLetter);
        keyRepository.save(config);

        KeyDTO response = new KeyDTO();
        response.setId(config.getId());
        response.setPrefix(config.getPrefix());
        response.setTargetDestiny(config.getTargetDestiny());
        response.setOldNumberKey(oldNumber);
        response.setOldLetterKey(oldLetter);
        response.setNewNumberKey(newNumber);
        response.setNewLetterKey(newLetter);
        response.setCompletKey(buildCompleteKey(config.getPrefix(), newLetter, newNumber));
        return response;
    }

    private int normalizeNumber(Integer value) {
        if (value == null || value < 0 || value > MAX_NUMERIC) {
            return 0;
        }
        return value;
    }

    private String normalizeLetter(String value) {
        if (!StringUtils.hasText(value)) {
            return MIN_LETTER;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 4) {
            return MIN_LETTER;
        }

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c < 'A' || c > 'Z') {
                return MIN_LETTER;
            }
        }
        return normalized;
    }

    private String incrementLetter(String current) {
        String normalized = normalizeLetter(current);
        if (MAX_LETTER.equals(normalized)) {
            return MIN_LETTER;
        }

        char[] chars = normalized.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] < 'Z') {
                chars[i] = (char) (chars[i] + 1);
                for (int j = i + 1; j < chars.length; j++) {
                    chars[j] = 'A';
                }
                return new String(chars);
            }
        }
        return MIN_LETTER;
    }

    private String buildCompleteKey(String prefix, String letter, int number) {
        return prefix + letter + String.format(Locale.ROOT, "%07d", number);
    }
}
