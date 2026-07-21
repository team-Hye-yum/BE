package site.dataon.hyeyum.dto;

import jakarta.validation.constraints.Pattern;

public record SupportProgramPeriod(
        @Pattern(regexp = "^(\\d{8}|\\d{4}-\\d{2}-\\d{2})$") String startDate,
        @Pattern(regexp = "^(\\d{8}|\\d{4}-\\d{2}-\\d{2})$") String endDate) {}
