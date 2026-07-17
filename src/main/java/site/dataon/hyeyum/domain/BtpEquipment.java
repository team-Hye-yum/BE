package site.dataon.hyeyum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(
        name = "btp_equipment",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_btp_equipment_source_seq",
                        columnNames = {"source_equipment_seq"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BtpEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시퀀스//장비 내부 식별자")
    @Column(name = "id", nullable = false)
    private Long id;

    @Comment("원본 장비 순번(eq_seq)")
    @Column(name = "source_equipment_seq", nullable = false)
    private Integer sourceEquipmentSeq;

    @Comment("장비명")
    @Column(name = "equipment_name", nullable = false, length = 255)
    private String equipmentName;

    @Comment("영문 장비명")
    @Column(name = "equipment_name_en", length = 255)
    private String equipmentNameEn;

    @Comment("표준분류 대분류")
    @Column(name = "category_large", length = 100)
    private String categoryLarge;

    @Comment("표준분류 중분류")
    @Column(name = "category_middle", length = 100)
    private String categoryMiddle;

    @Comment("표준분류 소분류")
    @Column(name = "category_small", length = 100)
    private String categorySmall;

    @Comment("설치장소명")
    @Column(name = "location_name", length = 255)
    private String locationName;

    @Comment("설치장소 위도")
    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Comment("설치장소 경도")
    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Comment("장비 이미지 URL")
    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Comment("장비 설명")
    @Column(name = "description", columnDefinition = "text")
    private String description;
}
