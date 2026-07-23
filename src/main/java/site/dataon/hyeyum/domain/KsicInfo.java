package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ksic_info")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KsicInfo {

    @Id
    @Column(name = "ksic_code", nullable = false, length = 20)
    private String ksicCode;

    @Column(name = "industry_description", columnDefinition = "text")
    private String industryDescription;

    @Column(name = "section_code", length = 1)
    private String sectionCode;

    @Column(name = "section_name", length = 200)
    private String sectionName;

    @Column(name = "division_code", length = 2)
    private String divisionCode;

    @Column(name = "division_name", length = 300)
    private String divisionName;

    @Column(name = "group_code", length = 3)
    private String groupCode;

    @Column(name = "group_name", length = 300)
    private String groupName;

    @Column(name = "class_code", length = 4)
    private String classCode;

    @Column(name = "class_name", length = 300)
    private String className;

    @Column(name = "subclass_code", length = 5)
    private String subclassCode;

    @Column(name = "subclass_name", length = 300)
    private String subclassName;
}
