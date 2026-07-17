package site.dataon.hyeyum.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.StringJoiner;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

final class ExcelImportSupport {

    private static final DataFormatter FORMATTER = new DataFormatter();
    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ExcelImportSupport() {}

    static String text(Row row, int index) {
        if (row == null || index < 0) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        String value = FORMATTER.formatCellValue(cell);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isBlank() ? null : value;
    }

    static Integer integer(Row row, int index) {
        String value = text(row, index);
        if (value == null) {
            return null;
        }
        value = value.replace(",", "").replace(" ", "");
        if (value.isBlank() || "-".equals(value)) {
            return null;
        }
        return new BigDecimal(value).intValue();
    }

    static Double decimal(Row row, int index) {
        String value = text(row, index);
        if (value == null) {
            return null;
        }
        value = value.replace(",", "").replace(" ", "");
        if (value.isBlank() || "-".equals(value)) {
            return null;
        }
        return Double.valueOf(value);
    }

    static Boolean koreanBoolean(Row row, int index) {
        String value = text(row, index);
        if (value == null) {
            return null;
        }
        return switch (value.toUpperCase()) {
            case "Y", "YES", "1", "O", "유", "여", "TRUE" -> Boolean.TRUE;
            case "N", "NO", "0", "X", "무", "부", "FALSE" -> Boolean.FALSE;
            default -> null;
        };
    }

    static LocalDate date(Row row, int index) {
        if (row == null || index < 0) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = text(row, index);
        if (value == null || "-".equals(value)) {
            return null;
        }
        if (value.endsWith(".0")) {
            value = value.substring(0, value.length() - 2);
        }
        if (value.length() >= 19 && value.charAt(4) == '-') {
            return LocalDateTime.parse(value.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate();
        }
        if (value.length() >= 10 && value.charAt(4) == '-') {
            return LocalDate.parse(value.substring(0, 10));
        }
        if (value.length() >= 10 && value.charAt(4) == '.') {
            String dottedDate = value.substring(0, 10);
            if (dottedDate.endsWith(".")) {
                dottedDate = dottedDate.substring(0, dottedDate.length() - 1);
            }
            return LocalDate.parse(dottedDate, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        }
        if (value.length() == 8 && value.chars().allMatch(Character::isDigit)) {
            return LocalDate.parse(value, COMPACT_DATE);
        }
        return null;
    }

    static String hash(Object... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringJoiner joiner = new StringJoiner("|");
            for (Object value : values) {
                joiner.add(value == null ? "" : value.toString().trim());
            }
            return HexFormat.of().formatHex(digest.digest(joiner.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
