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
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "source_equipment_seq", nullable = false)
    private Integer sourceEquipmentSeq;

    @Column(name = "equipment_name", nullable = false, length = 255)
    private String equipmentName;

    @Column(name = "equipment_name_en", length = 255)
    private String equipmentNameEn;

    @Column(name = "category_large", length = 100)
    private String categoryLarge;

    @Column(name = "category_middle", length = 100)
    private String categoryMiddle;

    @Column(name = "category_small", length = 100)
    private String categorySmall;

    @Column(name = "location_name", length = 255)
    private String locationName;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;
}
