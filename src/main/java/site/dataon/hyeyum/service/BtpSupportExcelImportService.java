package site.dataon.hyeyum.service;

import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.dataon.hyeyum.dto.DataImportResult;

@Service
public class BtpSupportExcelImportService {

    private final JdbcTemplate jdbcTemplate;

    public BtpSupportExcelImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public DataImportResult importFile(MultipartFile file) {
        DataImportCounter counter = new DataImportCounter();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            for (Sheet sheet : workbook) {
                if (sheet.getSheetName().endsWith("_사업목록")) {
                    importProgramSheet(sheet, counter);
                }
                if (sheet.getSheetName().endsWith("_기업지원목록")) {
                    importSupportHistorySheet(sheet, counter);
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("BTP 엑셀 파일을 읽을 수 없습니다.", exception);
        }
        return new DataImportResult(file.getOriginalFilename(), counter.snapshot());
    }

    private void importProgramSheet(Sheet sheet, DataImportCounter counter) {
        Integer programYear = yearFromSheetName(sheet);
        if (programYear == null) {
            return;
        }
        for (Row row : sheet) {
            if (row.getRowNum() < 3) {
                continue;
            }
            String code = ExcelImportSupport.text(row, 1);
            if (code == null) {
                continue;
            }
            jdbcTemplate.update(
                    """
                    insert into btp_support_program (
                        program_year, code, budget_program_name, program_category, support_type,
                        start_date, end_date, department_name, local_government_name, program_summary
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (program_year, code) do update set
                        budget_program_name = excluded.budget_program_name,
                        program_category = excluded.program_category,
                        support_type = excluded.support_type,
                        start_date = excluded.start_date,
                        end_date = excluded.end_date,
                        department_name = excluded.department_name,
                        local_government_name = excluded.local_government_name,
                        program_summary = excluded.program_summary
                    """,
                    programYear,
                    code,
                    ExcelImportSupport.text(row, 2),
                    ExcelImportSupport.text(row, 3),
                    ExcelImportSupport.text(row, 4),
                    ExcelImportSupport.date(row, 5),
                    ExcelImportSupport.date(row, 6),
                    ExcelImportSupport.text(row, 7),
                    ExcelImportSupport.text(row, 8),
                    ExcelImportSupport.text(row, 9));
            counter.increment("btp_support_program");
        }
    }

    private void importSupportHistorySheet(Sheet sheet, DataImportCounter counter) {
        Integer supportYear = yearFromSheetName(sheet);
        if (supportYear == null) {
            return;
        }
        for (Row row : sheet) {
            if (row.getRowNum() < 3) {
                continue;
            }
            String code = ExcelImportSupport.text(row, 1);
            if (code == null) {
                continue;
            }
            int selectedDateIndex = ExcelImportSupport.date(row, 7) != null || ExcelImportSupport.date(row, 8) == null ? 7 : 8;
            int selectionResultIndex = selectedDateIndex + 1;
            int supportAmountIndex = selectedDateIndex + 2;
            int startDateIndex = selectedDateIndex + 3;
            int endDateIndex = selectedDateIndex + 4;
            int companyIdIndex = selectedDateIndex + 5;
            int industryCodeIndex = selectedDateIndex + 6;
            int provinceNameIndex = selectedDateIndex + 7;
            int districtNameIndex = selectedDateIndex + 8;
            int mainProductIndex = selectedDateIndex + 9;
            int establishedYearIndex = selectedDateIndex + 10;

            Integer companyId = ExcelImportSupport.integer(row, companyIdIndex);
            if (companyId == null) {
                continue;
            }
            String sourceHash = ExcelImportSupport.hash(
                    supportYear,
                    code,
                    companyId,
                    ExcelImportSupport.date(row, selectedDateIndex),
                    ExcelImportSupport.integer(row, supportAmountIndex),
                    ExcelImportSupport.text(row, 6));
            jdbcTemplate.update(
                    """
                    insert into btp_support_history (
                        support_year, code, budget_program_name, support_type, support_category,
                        support_detail, support_item, selected_date, selection_result,
                        support_amount, start_date, end_date, company_id, industry_code,
                        province_name, district_name, main_product, established_year, source_hash
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (source_hash) do update set
                        budget_program_name = excluded.budget_program_name,
                        support_type = excluded.support_type,
                        support_category = excluded.support_category,
                        support_detail = excluded.support_detail,
                        selection_result = excluded.selection_result,
                        start_date = excluded.start_date,
                        end_date = excluded.end_date,
                        industry_code = excluded.industry_code,
                        province_name = excluded.province_name,
                        district_name = excluded.district_name,
                        main_product = excluded.main_product,
                        established_year = excluded.established_year
                    """,
                    supportYear,
                    code,
                    ExcelImportSupport.text(row, 2),
                    ExcelImportSupport.text(row, 3),
                    ExcelImportSupport.text(row, 4),
                    ExcelImportSupport.text(row, 5),
                    ExcelImportSupport.text(row, 6),
                    ExcelImportSupport.date(row, selectedDateIndex),
                    ExcelImportSupport.text(row, selectionResultIndex),
                    ExcelImportSupport.integer(row, supportAmountIndex),
                    ExcelImportSupport.date(row, startDateIndex),
                    ExcelImportSupport.date(row, endDateIndex),
                    companyId,
                    ExcelImportSupport.text(row, industryCodeIndex),
                    ExcelImportSupport.text(row, provinceNameIndex),
                    ExcelImportSupport.text(row, districtNameIndex),
                    ExcelImportSupport.text(row, mainProductIndex),
                    ExcelImportSupport.integer(row, establishedYearIndex),
                    sourceHash);
            counter.increment("btp_support_history");
        }
    }

    private Integer yearFromSheetName(Sheet sheet) {
        String name = sheet.getSheetName();
        if (name.length() < 4) {
            return null;
        }
        try {
            return Integer.valueOf(name.substring(0, 4));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
