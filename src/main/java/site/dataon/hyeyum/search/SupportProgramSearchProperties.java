package site.dataon.hyeyum.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "support-program-search.elasticsearch")
public record SupportProgramSearchProperties(
        boolean enabled,
        boolean startupSyncEnabled,
        int startupSyncInitialDelaySeconds,
        int startupSyncMaxAttempts,
        int startupSyncRetryDelaySeconds,
        int resultSize) {}
