package site.dataon.hyeyum.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import site.dataon.hyeyum.repository.BtpSupportProgramRepository;
import site.dataon.hyeyum.service.CompanyMetricCalculationService;

@Component
public class SupportProgramSearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(SupportProgramSearchIndexer.class);

    private final BtpSupportProgramRepository supportProgramRepository;
    private final SupportProgramElasticsearchService elasticsearchService;
    private final SupportProgramSearchProperties properties;
    private final CompanyMetricCalculationService companyMetricCalculationService;

    public SupportProgramSearchIndexer(
            BtpSupportProgramRepository supportProgramRepository,
            SupportProgramElasticsearchService elasticsearchService,
            SupportProgramSearchProperties properties,
            CompanyMetricCalculationService companyMetricCalculationService) {
        this.supportProgramRepository = supportProgramRepository;
        this.elasticsearchService = elasticsearchService;
        this.properties = properties;
        this.companyMetricCalculationService = companyMetricCalculationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        Thread syncThread = new Thread(this::syncOnStartupInBackground, "support-program-elasticsearch-sync");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void syncOnStartupInBackground() {
        try {
            syncSupportProgramsToElasticsearch();
        } finally {
            recalculateCompanyMetrics();
        }
    }

    private void syncSupportProgramsToElasticsearch() {
        if (!properties.enabled() || !properties.startupSyncEnabled()) {
            log.info("Skipping Elasticsearch support program startup sync. enabled={}, startupSyncEnabled={}", properties.enabled(), properties.startupSyncEnabled());
            return;
        }
        var programs = supportProgramRepository.findAll();
        sleepInitialDelay();
        int maxAttempts = Math.max(1, properties.startupSyncMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (elasticsearchService.reindexAll(programs)) {
                log.info("Synced {} support programs to Elasticsearch.", programs.size());
                return;
            }
            if (attempt < maxAttempts) {
                sleepBeforeRetry(attempt, maxAttempts);
            }
        }
        log.warn("Elasticsearch sync gave up after {} attempts. Search will fall back to PostgreSQL until Elasticsearch is reachable.", maxAttempts);
    }

    private void recalculateCompanyMetrics() {
        try {
            log.info("Recalculating company metrics after Elasticsearch startup sync.");
            var result = companyMetricCalculationService.recalculateAllWithoutAi();
            log.info(
                    "Recalculated company metrics after startup sync. totalCompanies={}, updatedCompanies={}, errors={}",
                    result.totalCompanyCount(),
                    result.updatedCompanyCount(),
                    result.failedCompanyCount());
        } catch (RuntimeException exception) {
            log.warn("Failed to recalculate company metrics after Elasticsearch startup sync.", exception);
        }
    }

    private void sleepInitialDelay() {
        int delaySeconds = Math.max(0, properties.startupSyncInitialDelaySeconds());
        if (delaySeconds == 0) {
            return;
        }
        log.info("Waiting {} seconds before Elasticsearch support program sync.", delaySeconds);
        sleep(delaySeconds);
    }

    private void sleepBeforeRetry(int attempt, int maxAttempts) {
        int delaySeconds = Math.max(1, properties.startupSyncRetryDelaySeconds());
        log.info("Retrying Elasticsearch support program sync in {} seconds. attempt={}/{}", delaySeconds, attempt + 1, maxAttempts);
        sleep(delaySeconds);
    }

    private void sleep(int delaySeconds) {
        try {
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch startup sync was interrupted.", exception);
        }
    }
}
