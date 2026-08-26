package com.readingledger.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readingledger.domain.EvidenceSnapshotItem;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class EvidenceSnapshotConverter implements AttributeConverter<List<EvidenceSnapshotItem>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<EvidenceSnapshotItem>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<EvidenceSnapshotItem> attribute) {
        if (attribute == null) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize evidence snapshot", e);
        }
    }

    @Override
    public List<EvidenceSnapshotItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize evidence snapshot", e);
        }
    }
}
