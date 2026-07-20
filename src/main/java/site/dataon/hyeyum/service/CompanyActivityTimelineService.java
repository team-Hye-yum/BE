package site.dataon.hyeyum.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import site.dataon.hyeyum.domain.BtpSupportHistory;
import site.dataon.hyeyum.domain.CompanyNtisCollaborativeProject;
import site.dataon.hyeyum.domain.CompanyNtisLeadProject;
import site.dataon.hyeyum.domain.CompanyPatent;
import site.dataon.hyeyum.dto.ApiDataResponse;
import site.dataon.hyeyum.dto.CompanyActivitySupportTimelineResponse;
import site.dataon.hyeyum.dto.CompanyActivitySupportTimelineResponse.EmptyMessage;
import site.dataon.hyeyum.dto.CompanyActivitySupportTimelineResponse.PointEventItem;
import site.dataon.hyeyum.dto.CompanyActivitySupportTimelineResponse.TimelineEventItem;
import site.dataon.hyeyum.dto.CompanyActivitySupportTimelineResponse.TimelineRange;
import site.dataon.hyeyum.repository.BtpSupportHistoryRepository;
import site.dataon.hyeyum.repository.CompanyNtisCollaborativeProjectRepository;
import site.dataon.hyeyum.repository.CompanyNtisLeadProjectRepository;
import site.dataon.hyeyum.repository.CompanyPatentRepository;
import site.dataon.hyeyum.repository.CompanyRepository;

@Service
public class CompanyActivityTimelineService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String SELECTED_RESULT = "지원대상";
    private static final String PATENT_REGISTERED = "PATENT_REGISTERED";
    private static final String PATENT_APPLICATION = "PATENT_APPLICATION";
    private static final String NTIS_PROJECT_PERIOD = "NTIS_PROJECT_PERIOD";
    private static final String NTIS_COLLABORATIVE_EVENT = "NTIS_COLLABORATIVE_EVENT";
    private static final String BTP_SUPPORT = "BTP_SUPPORT";

    private final CompanyRepository companyRepository;
    private final CompanyPatentRepository patentRepository;
    private final CompanyNtisLeadProjectRepository ntisLeadProjectRepository;
    private final CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository;
    private final BtpSupportHistoryRepository supportHistoryRepository;

    public CompanyActivityTimelineService(
            CompanyRepository companyRepository,
            CompanyPatentRepository patentRepository,
            CompanyNtisLeadProjectRepository ntisLeadProjectRepository,
            CompanyNtisCollaborativeProjectRepository ntisCollaborativeProjectRepository,
            BtpSupportHistoryRepository supportHistoryRepository) {
        this.companyRepository = companyRepository;
        this.patentRepository = patentRepository;
        this.ntisLeadProjectRepository = ntisLeadProjectRepository;
        this.ntisCollaborativeProjectRepository = ntisCollaborativeProjectRepository;
        this.supportHistoryRepository = supportHistoryRepository;
    }

    @Transactional(readOnly = true)
    public ApiDataResponse<CompanyActivitySupportTimelineResponse> activitySupportTimeline(Integer companyId) {
        verifyCompanyExists(companyId);

        List<CompanyPatent> patents = patentRepository.findByCompanyIdOrderByRegistrationDateDescPatentIdAsc(companyId);
        List<CompanyNtisLeadProject> leadProjects =
                ntisLeadProjectRepository.findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisLeadProjectIdAsc(
                        companyId);
        List<CompanyNtisCollaborativeProject> collaborativeProjects = ntisCollaborativeProjectRepository
                .findByCompanyIdOrderByReferenceYearDescReferenceDateDescNtisCollaborativeProjectIdAsc(companyId);
        List<BtpSupportHistory> supportHistories = supportHistoryRepository
                .findByCompanyIdOrderBySupportYearAscSupportHistoryIdAsc(companyId)
                .stream()
                .filter(history -> sameText(history.getSelectionResult(), SELECTED_RESULT))
                .toList();

        List<PointEventItem> patentEvents = patentEvents(patents);
        List<TimelineEventItem> ntisEvents = ntisEvents(leadProjects, collaborativeProjects);
        List<PointEventItem> btpSupportEvents = btpSupportEvents(supportHistories);
        TimelineRange timelineRange = timelineRange(patentEvents, ntisEvents, btpSupportEvents);
        List<EmptyMessage> emptyMessages = emptyMessages(patentEvents, ntisEvents);

        return new ApiDataResponse<>(new CompanyActivitySupportTimelineResponse(
                timelineRange,
                patentEvents,
                ntisEvents,
                btpSupportEvents,
                emptyMessages));
    }

    private List<PointEventItem> patentEvents(List<CompanyPatent> patents) {
        Map<PointEventKey, Integer> counts = new LinkedHashMap<>();
        patents.forEach(patent -> {
            if (patent.getRegistrationDate() != null && sameText(patent.getRegistrationStatus(), "등록")) {
                increment(counts, new PointEventKey(PATENT_REGISTERED, patent.getRegistrationDate().getYear(), patent.getRegistrationDate()));
            }
            if (patent.getApplicationDate() != null) {
                increment(counts, new PointEventKey(PATENT_APPLICATION, patent.getApplicationDate().getYear(), patent.getApplicationDate()));
            }
        });
        return counts.entrySet().stream()
                .map(entry -> pointEvent(
                        entry.getKey().eventType(), entry.getKey().eventYear(), entry.getKey().eventDate(), entry.getValue(), "건"))
                .sorted(pointEventComparator())
                .toList();
    }

    private List<TimelineEventItem> ntisEvents(
            List<CompanyNtisLeadProject> leadProjects,
            List<CompanyNtisCollaborativeProject> collaborativeProjects) {
        List<TimelineEventItem> events = new ArrayList<>();
        Map<PeriodEventKey, Integer> periodCounts = new LinkedHashMap<>();
        leadProjects.forEach(project -> {
            LocalDate startDate = firstPresent(project.getTotalResearchStartDate(), project.getAnnualResearchStartDate());
            LocalDate endDate = firstPresent(project.getTotalResearchEndDate(), project.getAnnualResearchEndDate());
            if (startDate != null && endDate != null) {
                increment(periodCounts, new PeriodEventKey(startDate, endDate));
            }
        });
        periodCounts.entrySet().stream()
                .map(entry -> periodEvent(entry.getKey().startDate(), entry.getKey().endDate(), entry.getValue()))
                .forEach(events::add);

        Map<PointEventKey, Integer> collaborativeCounts = new LinkedHashMap<>();
        collaborativeProjects.forEach(project -> increment(
                collaborativeCounts,
                new PointEventKey(
                        NTIS_COLLABORATIVE_EVENT,
                        project.getReferenceYear(),
                        eventDate(project.getReferenceDate(), project.getReferenceYear()))));
        collaborativeCounts.entrySet().stream()
                .filter(entry -> entry.getKey().eventYear() != null)
                .map(entry -> pointTimelineEvent(
                        entry.getKey().eventType(), entry.getKey().eventYear(), entry.getKey().eventDate(), entry.getValue()))
                .forEach(events::add);

        return events.stream()
                .sorted(timelineEventComparator())
                .toList();
    }

    private List<PointEventItem> btpSupportEvents(List<BtpSupportHistory> histories) {
        Map<PointEventKey, Integer> counts = new LinkedHashMap<>();
        histories.forEach(history -> {
            LocalDate eventDate = eventDate(firstPresent(history.getSelectedDate(), history.getStartDate()), history.getSupportYear());
            if (eventDate != null) {
                increment(counts, new PointEventKey(BTP_SUPPORT, eventDate.getYear(), eventDate));
            }
        });
        return counts.entrySet().stream()
                .map(entry -> pointEvent(
                        entry.getKey().eventType(), entry.getKey().eventYear(), entry.getKey().eventDate(), entry.getValue(), "건"))
                .sorted(pointEventComparator())
                .toList();
    }

    private PointEventItem pointEvent(String eventType, Integer eventYear, LocalDate eventDate, int count, String suffix) {
        return new PointEventItem(
                eventType,
                eventYear,
                formatDate(eventDate),
                count,
                label(count, suffix));
    }

    private TimelineEventItem periodEvent(LocalDate startDate, LocalDate endDate, int count) {
        return new TimelineEventItem(
                NTIS_PROJECT_PERIOD,
                null,
                null,
                startDate.getYear(),
                endDate.getYear(),
                formatDate(startDate),
                formatDate(endDate),
                count,
                "과제 " + count + "건");
    }

    private TimelineEventItem pointTimelineEvent(String eventType, Integer eventYear, LocalDate eventDate, int count) {
        return new TimelineEventItem(
                eventType,
                eventYear,
                formatDate(eventDate),
                null,
                null,
                null,
                null,
                count,
                "과제 " + count + "건");
    }

    private TimelineRange timelineRange(
            List<PointEventItem> patentEvents,
            List<TimelineEventItem> ntisEvents,
            List<PointEventItem> btpSupportEvents) {
        List<Integer> years = new ArrayList<>();
        patentEvents.forEach(event -> years.add(event.eventYear()));
        btpSupportEvents.forEach(event -> years.add(event.eventYear()));
        ntisEvents.forEach(event -> {
            addIfPresent(years, event.eventYear());
            addIfPresent(years, event.startYear());
            addIfPresent(years, event.endYear());
        });
        List<Integer> presentYears = years.stream().filter(Objects::nonNull).toList();
        if (presentYears.isEmpty()) {
            return null;
        }
        return new TimelineRange(
                presentYears.stream().min(Integer::compareTo).orElse(null),
                presentYears.stream().max(Integer::compareTo).orElse(null));
    }

    private List<EmptyMessage> emptyMessages(List<PointEventItem> patentEvents, List<TimelineEventItem> ntisEvents) {
        if (!patentEvents.isEmpty() || !ntisEvents.isEmpty()) {
            return List.of();
        }
        return List.of(new EmptyMessage("NO_ACTIVITY_HISTORY", "관련 활동 이력이 확인되지 않음"));
    }

    private void verifyCompanyExists(Integer companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "기업 정보를 찾을 수 없습니다.");
        }
    }

    private void increment(Map<PointEventKey, Integer> counts, PointEventKey key) {
        if (key.eventYear() != null) {
            counts.merge(key, 1, Integer::sum);
        }
    }

    private void increment(Map<PeriodEventKey, Integer> counts, PeriodEventKey key) {
        counts.merge(key, 1, Integer::sum);
    }

    private LocalDate firstPresent(LocalDate first, LocalDate second) {
        return first != null ? first : second;
    }

    private LocalDate eventDate(LocalDate date, Integer year) {
        if (date != null) {
            return date;
        }
        return year == null ? null : LocalDate.of(year, 1, 1);
    }

    private void addIfPresent(List<Integer> years, Integer year) {
        if (year != null) {
            years.add(year);
        }
    }

    private Comparator<PointEventItem> pointEventComparator() {
        return Comparator.comparing(PointEventItem::eventYear, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PointEventItem::eventDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PointEventItem::eventType);
    }

    private Comparator<TimelineEventItem> timelineEventComparator() {
        return Comparator.comparing(this::eventStartYear, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(this::eventStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TimelineEventItem::eventType);
    }

    private Integer eventStartYear(TimelineEventItem event) {
        return event.startYear() != null ? event.startYear() : event.eventYear();
    }

    private String eventStartDate(TimelineEventItem event) {
        return event.startDate() != null ? event.startDate() : event.eventDate();
    }

    private boolean sameText(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return !normalizedLeft.isBlank() && Objects.equals(normalizedLeft, normalizedRight);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private String label(int count, String suffix) {
        return count + suffix;
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : BASIC_DATE.format(date);
    }

    private record PointEventKey(String eventType, Integer eventYear, LocalDate eventDate) {}

    private record PeriodEventKey(LocalDate startDate, LocalDate endDate) {}
}
