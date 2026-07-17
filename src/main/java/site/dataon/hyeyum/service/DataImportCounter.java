package site.dataon.hyeyum.service;

import java.util.LinkedHashMap;
import java.util.Map;

final class DataImportCounter {

    private final Map<String, Integer> counts = new LinkedHashMap<>();

    void increment(String tableName) {
        counts.merge(tableName, 1, Integer::sum);
    }

    Map<String, Integer> snapshot() {
        return Map.copyOf(counts);
    }
}
