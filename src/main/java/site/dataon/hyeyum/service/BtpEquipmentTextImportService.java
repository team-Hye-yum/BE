package site.dataon.hyeyum.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.dto.DataImportResult;

@Service
public class BtpEquipmentTextImportService {

    private static final String BLOCK_DELIMITER = "(?m)^={80}\\R?";

    private final JdbcTemplate jdbcTemplate;

    public BtpEquipmentTextImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public DataImportResult importFile(MultipartFile file) {
        DataImportCounter counter = new DataImportCounter();
        String text = readUtf8(file);
        for (String block : text.split(BLOCK_DELIMITER)) {
            BtpEquipmentRow row = parseBlock(block);
            if (row == null) {
                continue;
            }
            upsert(row);
            counter.increment("btp_equipment");
        }
        return new DataImportResult(file.getOriginalFilename(), counter.snapshot());
    }

    private String readUtf8(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("BTP equipment text file could not be read.", exception);
        }
    }

    private BtpEquipmentRow parseBlock(String block) {
        String normalizedBlock = block.strip();
        if (normalizedBlock.isEmpty()) {
            return null;
        }

        Integer sourceEquipmentSeq = null;
        String equipmentName = null;
        String equipmentNameEn = null;
        String categoryLarge = null;
        String categoryMiddle = null;
        String categorySmall = null;
        String locationName = null;
        String imageUrl = null;
        String description = null;

        String section = null;
        List<String> descriptionLines = new ArrayList<>();

        for (String rawLine : normalizedBlock.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1);
                continue;
            }

            if (line.startsWith("eq_seq:")) {
                sourceEquipmentSeq = integerAfterColon(line);
                continue;
            }
            if (line.startsWith("장비명:")) {
                equipmentName = textAfterColon(line);
                continue;
            }
            if (line.startsWith("영문명:")) {
                equipmentNameEn = textAfterColon(line);
                continue;
            }
            if (line.startsWith("표준분류:")) {
                String[] categories = textAfterColon(line).split("\\s*>\\s*");
                categoryLarge = categoryAt(categories, 0);
                categoryMiddle = categoryAt(categories, 1);
                categorySmall = categoryAt(categories, 2);
                continue;
            }
            if (line.startsWith("설치장소")) {
                locationName = textAfterColon(line);
                continue;
            }
            if ("이미지".equals(section) && imageUrl == null && line.startsWith("http")) {
                imageUrl = line;
                continue;
            }
            if ("장비설명".equals(section)) {
                descriptionLines.add(line);
            }
        }

        if (!descriptionLines.isEmpty()) {
            description = String.join(System.lineSeparator(), descriptionLines);
        }
        if (sourceEquipmentSeq == null || equipmentName == null) {
            return null;
        }

        return new BtpEquipmentRow(
                sourceEquipmentSeq,
                equipmentName,
                equipmentNameEn,
                categoryLarge,
                categoryMiddle,
                categorySmall,
                locationName,
                null,
                null,
                imageUrl,
                description);
    }

    private void upsert(BtpEquipmentRow row) {
        jdbcTemplate.update(
                """
                insert into btp_equipment (
                    source_equipment_seq, equipment_name, equipment_name_en,
                    category_large, category_middle, category_small,
                    location_name, latitude, longitude, image_url, description
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (source_equipment_seq) do update set
                    equipment_name = excluded.equipment_name,
                    equipment_name_en = excluded.equipment_name_en,
                    category_large = excluded.category_large,
                    category_middle = excluded.category_middle,
                    category_small = excluded.category_small,
                    location_name = excluded.location_name,
                    latitude = coalesce(excluded.latitude, btp_equipment.latitude),
                    longitude = coalesce(excluded.longitude, btp_equipment.longitude),
                    image_url = excluded.image_url,
                    description = excluded.description
                """,
                row.sourceEquipmentSeq(),
                row.equipmentName(),
                row.equipmentNameEn(),
                row.categoryLarge(),
                row.categoryMiddle(),
                row.categorySmall(),
                row.locationName(),
                row.latitude(),
                row.longitude(),
                row.imageUrl(),
                row.description());
    }

    private Integer integerAfterColon(String line) {
        String value = textAfterColon(line);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String textAfterColon(String line) {
        int index = line.indexOf(':');
        if (index < 0 || index == line.length() - 1) {
            return null;
        }
        String value = line.substring(index + 1).strip();
        return value.isEmpty() ? null : value;
    }

    private String categoryAt(String[] categories, int index) {
        if (index >= categories.length) {
            return null;
        }
        String value = categories[index].strip();
        return value.isEmpty() ? null : value;
    }

    private record BtpEquipmentRow(
            Integer sourceEquipmentSeq,
            String equipmentName,
            String equipmentNameEn,
            String categoryLarge,
            String categoryMiddle,
            String categorySmall,
            String locationName,
            BigDecimal latitude,
            BigDecimal longitude,
            String imageUrl,
            String description) {}
}
