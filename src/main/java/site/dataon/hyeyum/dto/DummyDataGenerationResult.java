package site.dataon.hyeyum.dto;

import java.util.List;
import java.util.Map;

public record DummyDataGenerationResult(
        Integer programYear,
        String programCode,
        List<Integer> companyIds,
        Map<String, Integer> insertCounts) {}
