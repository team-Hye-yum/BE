package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.dataon.hyeyum.domain.BtpSupportHistory;

public interface BtpSupportHistoryRepository extends JpaRepository<BtpSupportHistory, Long> {

    List<BtpSupportHistory> findByCompanyIdOrderBySupportYearAscSupportHistoryIdAsc(Integer companyId);

    int countByCompanyIdAndSelectionResult(Integer companyId, String selectionResult);

    @Query("select max(h.supportYear) from BtpSupportHistory h where h.companyId = :companyId")
    Integer findMaxSupportYear(@Param("companyId") Integer companyId);

    List<BtpSupportHistory> findByCompanyIdAndSupportYearOrderBySelectedDateDescSupportHistoryIdDesc(
            Integer companyId, Integer supportYear);

    List<BtpSupportHistory> findByCompanyIdAndSupportYearLessThanOrderBySupportYearDescSelectedDateDescSupportHistoryIdDesc(
            Integer companyId, Integer supportYear);

    @Query(
            """
            select h
            from BtpSupportHistory h
            where h.companyId = :companyId
            order by h.startDate desc nulls last, h.selectedDate desc nulls last, h.supportHistoryId desc
            """)
    List<BtpSupportHistory> findTimeline(@Param("companyId") Integer companyId);

    @Query(
            value =
                    """
                    select
                        support_year as supportYear,
                        support_type as supportType,
                        count(*)::int as supportCount
                    from btp_support_history
                    where company_id = :companyId
                    group by support_year, support_type
                    order by support_year, support_type
                    """,
            nativeQuery = true)
    List<SupportYearTypeCountProjection> countByYearAndSupportType(@Param("companyId") Integer companyId);
}
