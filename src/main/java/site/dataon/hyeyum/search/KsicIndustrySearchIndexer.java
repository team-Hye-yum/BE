package site.dataon.hyeyum.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import site.dataon.hyeyum.repository.KsicInfoRepository;

@Component
public class KsicIndustrySearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(KsicIndustrySearchIndexer.class);

    private final KsicInfoRepository ksicInfoRepository;
    private final KsicIndustryElasticsearchService elasticsearchService;
    private final SupportProgramSearchProperties properties;

    public KsicIndustrySearchIndexer(
            KsicInfoRepository ksicInfoRepository,
            KsicIndustryElasticsearchService elasticsearchService,
            SupportProgramSearchProperties properties) {
        this.ksicInfoRepository = ksicInfoRepository;
        this.elasticsearchService = elasticsearchService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        Thread syncThread = new Thread(this::syncOnStartupInBackground, "ksic-industry-elasticsearch-sync");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void syncOnStartupInBackground() {
        if (!properties.enabled() || !properties.startupSyncEnabled()) {
            log.info("Skipping Elasticsearch KSIC industry startup sync. enabled={}, startupSyncEnabled={}", properties.enabled(), properties.startupSyncEnabled());
            return;
        }
        var ksicInfos = ksicInfoRepository.findAll();
        sleep(Math.max(0, properties.startupSyncInitialDelaySeconds()));
        int maxAttempts = Math.max(1, properties.startupSyncMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (elasticsearchService.reindexAll(ksicInfos)) {
                log.info("Synced {} KSIC industries to Elasticsearch.", ksicInfos.size());
                return;
            }
            if (attempt < maxAttempts) {
                sleep(Math.max(1, properties.startupSyncRetryDelaySeconds()));
            }
        }
        log.warn("Elasticsearch KSIC industry sync gave up after {} attempts. Search will fall back to PostgreSQL.", maxAttempts);
    }

    private void sleep(int delaySeconds) {
        if (delaySeconds == 0) {
            return;
        }
        try {
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch KSIC industry startup sync was interrupted.", exception);
        }
    }
}
