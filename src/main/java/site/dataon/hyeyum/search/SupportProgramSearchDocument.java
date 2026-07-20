package site.dataon.hyeyum.search;

import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import site.dataon.hyeyum.domain.BtpSupportProgram;

@Document(indexName = "support-programs")
@Setting(settingPath = "/elasticsearch/support-program-settings.json")
public class SupportProgramSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String code;

    @Field(type = FieldType.Integer)
    private Integer programYear;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String budgetProgramName;

    @Field(type = FieldType.Keyword)
    private String programCategory;

    @Field(type = FieldType.Keyword)
    private String supportType;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate;

    @Field(type = FieldType.Keyword)
    private String departmentName;

    @Field(type = FieldType.Keyword)
    private String localGovernmentName;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String programSummary;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String searchText;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String searchChosung;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String searchJamo;

    protected SupportProgramSearchDocument() {}

    public static SupportProgramSearchDocument from(BtpSupportProgram program) {
        SupportProgramSearchDocument document = new SupportProgramSearchDocument();
        document.id = String.valueOf(program.getSupportProgramId());
        document.code = program.getCode();
        document.programYear = program.getProgramYear();
        document.budgetProgramName = program.getBudgetProgramName();
        document.programCategory = program.getProgramCategory();
        document.supportType = program.getSupportType();
        document.startDate = program.getStartDate();
        document.endDate = program.getEndDate();
        document.departmentName = program.getDepartmentName();
        document.localGovernmentName = program.getLocalGovernmentName();
        document.programSummary = program.getProgramSummary();
        document.searchText = KoreanSearchText.normalizedText(
                program.getBudgetProgramName(),
                program.getCode(),
                program.getProgramCategory(),
                program.getSupportType(),
                program.getDepartmentName(),
                program.getLocalGovernmentName(),
                program.getProgramSummary());
        document.searchChosung = KoreanSearchText.chosung(document.searchText);
        document.searchJamo = KoreanSearchText.jamo(document.searchText);
        return document;
    }

    public String getCode() {
        return code;
    }

    public Integer getProgramYear() {
        return programYear;
    }

    public String getBudgetProgramName() {
        return budgetProgramName;
    }

    public String getProgramCategory() {
        return programCategory;
    }

    public String getSupportType() {
        return supportType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getLocalGovernmentName() {
        return localGovernmentName;
    }
}
