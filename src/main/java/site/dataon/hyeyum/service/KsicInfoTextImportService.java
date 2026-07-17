package site.dataon.hyeyum.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.dto.DataImportResult;

@Service
public class KsicInfoTextImportService {

    private static final Pattern CODE_HEADER = Pattern.compile("^\\[([A-Z]\\d{5})]$");

    private final JdbcTemplate jdbcTemplate;

    public KsicInfoTextImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public DataImportResult importFile(MultipartFile file) {
        DataImportCounter counter = new DataImportCounter();
        String text = readUtf8(file);
        for (KsicInfoRow row : parse(text)) {
            upsert(row);
            counter.increment("ksic_info");
        }
        return new DataImportResult(file.getOriginalFilename(), counter.snapshot());
    }

    private String readUtf8(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("KSIC info text file could not be read.", exception);
        }
    }

    private List<KsicInfoRow> parse(String text) {
        List<KsicInfoRow> rows = new ArrayList<>();
        String currentCode = null;
        List<String> lines = new ArrayList<>();

        for (String rawLine : text.split("\\R")) {
            String line = rawLine.stripTrailing();
            Matcher matcher = CODE_HEADER.matcher(line.strip());
            if (matcher.matches()) {
                flush(rows, currentCode, lines);
                currentCode = matcher.group(1);
                lines.clear();
                continue;
            }
            if (currentCode != null) {
                lines.add(line);
            }
        }

        flush(rows, currentCode, lines);
        return rows;
    }

    private void flush(List<KsicInfoRow> rows, String code, List<String> lines) {
        if (code == null) {
            return;
        }

        String industryDescription = String.join(System.lineSeparator(), lines).strip();
        rows.add(new KsicInfoRow(code, industryDescription));
    }

    private void upsert(KsicInfoRow row) {
        jdbcTemplate.update(
                """
                insert into ksic_info (
                    ksic_code, industry_description
                ) values (?, ?)
                on conflict (ksic_code) do update set
                    industry_description = excluded.industry_description
                """,
                row.ksicCode(),
                row.industryDescription());
    }

    private record KsicInfoRow(String ksicCode, String industryDescription) {}
}
