package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.dataon.hyeyum.domain.KsicInfo;

public interface KsicInfoRepository extends JpaRepository<KsicInfo, String> {

    @Query(
            """
            select k
            from KsicInfo k
            where lower(replace(coalesce(k.ksicCode, ''), ' ', '')) like lower(concat('%', replace(:keyword, ' ', ''), '%'))
               or lower(replace(coalesce(k.sectionName, ''), ' ', '')) like lower(concat('%', replace(:keyword, ' ', ''), '%'))
               or lower(replace(coalesce(k.divisionName, ''), ' ', '')) like lower(concat('%', replace(:keyword, ' ', ''), '%'))
               or lower(replace(coalesce(k.groupName, ''), ' ', '')) like lower(concat('%', replace(:keyword, ' ', ''), '%'))
               or lower(replace(coalesce(k.className, ''), ' ', '')) like lower(concat('%', replace(:keyword, ' ', ''), '%'))
               or lower(replace(coalesce(k.subclassName, ''), ' ', '')) like lower(concat('%', replace(:keyword, ' ', ''), '%'))
            order by k.sectionCode, k.divisionCode, k.groupCode, k.classCode, k.subclassCode
            """)
    List<KsicInfo> searchByHierarchyText(@Param("keyword") String keyword);
}
