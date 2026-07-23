package site.dataon.hyeyum.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import site.dataon.hyeyum.domain.KsicInfo;

@Document(indexName = "ksic-industries")
@Setting(settingPath = "/elasticsearch/support-program-settings.json")
public class KsicIndustrySearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String ksicCode;

    @Field(type = FieldType.Keyword)
    private String sectionCode;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String sectionName;

    @Field(type = FieldType.Keyword)
    private String divisionCode;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String divisionName;

    @Field(type = FieldType.Keyword)
    private String groupCode;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String groupName;

    @Field(type = FieldType.Keyword)
    private String classCode;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String className;

    @Field(type = FieldType.Keyword)
    private String subclassCode;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String subclassName;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String displayName;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String searchText;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String searchChosung;

    @Field(type = FieldType.Text, analyzer = "support_ngram_analyzer", searchAnalyzer = "standard")
    private String searchJamo;

    protected KsicIndustrySearchDocument() {}

    public static KsicIndustrySearchDocument from(KsicInfo ksicInfo) {
        KsicIndustrySearchDocument document = new KsicIndustrySearchDocument();
        document.id = ksicInfo.getKsicCode();
        document.ksicCode = ksicInfo.getKsicCode();
        document.sectionCode = ksicInfo.getSectionCode();
        document.sectionName = ksicInfo.getSectionName();
        document.divisionCode = ksicInfo.getDivisionCode();
        document.divisionName = ksicInfo.getDivisionName();
        document.groupCode = ksicInfo.getGroupCode();
        document.groupName = ksicInfo.getGroupName();
        document.classCode = ksicInfo.getClassCode();
        document.className = ksicInfo.getClassName();
        document.subclassCode = ksicInfo.getSubclassCode();
        document.subclassName = ksicInfo.getSubclassName();
        document.displayName = displayName(ksicInfo);
        document.searchText = KoreanSearchText.normalizedText(
                document.ksicCode,
                document.sectionCode,
                document.sectionName,
                document.divisionCode,
                document.divisionName,
                document.groupCode,
                document.groupName,
                document.classCode,
                document.className,
                document.subclassCode,
                document.subclassName,
                document.displayName);
        document.searchChosung = KoreanSearchText.chosung(document.searchText);
        document.searchJamo = KoreanSearchText.jamo(document.searchText);
        return document;
    }

    static String displayName(KsicInfo ksicInfo) {
        return String.join(
                " > ",
                nullToBlank(ksicInfo.getSectionName()),
                nullToBlank(ksicInfo.getDivisionName()),
                nullToBlank(ksicInfo.getGroupName()),
                nullToBlank(ksicInfo.getClassName()),
                nullToBlank(ksicInfo.getSubclassName()));
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
