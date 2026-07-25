package site.dataon.hyeyum.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ExcelImportSupport {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportSupport.class);
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
        BigDecimal value = number(row, index);
        if (value == null) {
            return null;
        }
        try {
            return value.intValueExact();
        } catch (ArithmeticException exception) {
            log.warn(
                    "Ignoring out-of-range integer Excel cell. sheet={}, row={}, column={}, cell={}, value={}",
                    sheetName(row),
                    rowNumber(row),
                    index + 1,
                    cellAddress(row, index),
                    value);
            return null;
        }
    }

    static Long longInteger(Row row, int index) {
        BigDecimal value = number(row, index);
        if (value == null) {
            return null;
        }
        try {
            return value.longValueExact();
        } catch (ArithmeticException exception) {
            log.warn(
                    "Ignoring out-of-range long Excel cell. sheet={}, row={}, column={}, cell={}, value={}",
                    sheetName(row),
                    rowNumber(row),
                    index + 1,
                    cellAddress(row, index),
                    value);
            return null;
        }
    }

    static Double decimal(Row row, int index) {
        BigDecimal value = number(row, index);
        if (value == null) {
            return null;
        }
        return value.doubleValue();
    }

    private static BigDecimal number(Row row, int index) {
        String value = text(row, index);
        if (value == null) {
            return null;
        }
        value = value.replace(",", "").replace(" ", "");
        if (value.isBlank() || "-".equals(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            log.warn(
                    "Ignoring non-numeric Excel cell. sheet={}, row={}, column={}, cell={}, value={}",
                    sheetName(row),
                    rowNumber(row),
                    index + 1,
                    cellAddress(row, index),
                    value);
            return null;
        }
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
        try {
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
        } catch (DateTimeException exception) {
            log.warn(
                    "Ignoring invalid Excel date cell. sheet={}, row={}, column={}, cell={}, value={}",
                    sheetName(row),
                    rowNumber(row),
                    index + 1,
                    cellAddress(row, index),
                    value);
        }
        return null;
    }

    private static String sheetName(Row row) {
        return row == null || row.getSheet() == null ? null : row.getSheet().getSheetName();
    }

    private static Integer rowNumber(Row row) {
        return row == null ? null : row.getRowNum() + 1;
    }

    private static String cellAddress(Row row, int index) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        return cell == null ? null : cell.getAddress().formatAsString();
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
