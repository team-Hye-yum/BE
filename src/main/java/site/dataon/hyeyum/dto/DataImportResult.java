package site.dataon.hyeyum.dto;

import java.util.Map;

public record DataImportResult(String fileName, Map<String, Integer> upsertCounts) {}
