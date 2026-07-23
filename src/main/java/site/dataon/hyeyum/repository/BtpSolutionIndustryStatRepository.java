package site.dataon.hyeyum.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.dataon.hyeyum.domain.BtpSolutionIndustryStat;

public interface BtpSolutionIndustryStatRepository extends JpaRepository<BtpSolutionIndustryStat, Long> {

    @Query(
            """
            select max(s.year)
            from BtpSolutionIndustryStat s
            where s.sectionCode = :sectionCode
            """)
    Integer findLatestYearBySectionCode(@Param("sectionCode") String sectionCode);

    @Query(
            """
            select s
            from BtpSolutionIndustryStat s
            where s.sectionCode = :sectionCode
              and s.year = :year
              and s.statCategory = 'EMPLOYEE_SIZE'
              and s.regionName = '부산 전체'
              and s.middleIndustryName = ''
              and s.dimensionName = '계'
            """)
    Optional<BtpSolutionIndustryStat> findBusanScale(
            @Param("sectionCode") String sectionCode, @Param("year") Integer year);

    @Query(
            """
            select s
            from BtpSolutionIndustryStat s
            where s.sectionCode = :sectionCode
              and s.year = :year
              and s.statCategory = 'EMPLOYEE_SIZE'
              and s.regionName = '부산 전체'
              and s.middleIndustryName = ''
              and s.dimensionName in :dimensionNames
            """)
    List<BtpSolutionIndustryStat> findBusanEmployeeSizeStats(
            @Param("sectionCode") String sectionCode,
            @Param("year") Integer year,
            @Param("dimensionNames") Collection<String> dimensionNames);

    @Query(
            """
            select s
            from BtpSolutionIndustryStat s
            where s.sectionCode = :sectionCode
              and s.year = :year
              and s.statCategory = 'ORGANIZATION_FORM'
              and s.regionName = '부산 전체'
              and s.middleIndustryName = ''
            """)
    List<BtpSolutionIndustryStat> findBusanOrganizationStats(
            @Param("sectionCode") String sectionCode, @Param("year") Integer year);
}
