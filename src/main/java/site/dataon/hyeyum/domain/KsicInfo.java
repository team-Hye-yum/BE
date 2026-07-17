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
}
