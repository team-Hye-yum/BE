package site.dataon.hyeyum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.dataon.hyeyum.domain.BtpSupportHistory;

public interface BtpSupportHistoryRepository extends JpaRepository<BtpSupportHistory, Long> {

    List<BtpSupportHistory> findByCompanyIdOrderBySupportYearAscSupportHistoryIdAsc(Integer companyId);
}
