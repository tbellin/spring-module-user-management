package org.jbelt.module.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;

@Component
public class StartupBanner {

    private static final Logger log = LoggerFactory.getLogger(StartupBanner.class);

    private final Environment environment;

    public StartupBanner(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printBanner() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDev = Arrays.asList(activeProfiles).contains("dev");
        boolean isProd = Arrays.asList(activeProfiles).contains("prod");

        log.info("================================================");

        if (isDev) {
            log.info("[DEV MODE] Using H2 in-memory database");
            log.info("  Active profile: dev");

            File envFile = new File(".env");
            if (envFile.exists()) {
                log.info("  .env file: found");
            } else {
                log.info("  .env file: not found");
            }

            String[] generatedFiles = {
                "compose.yaml", "Dockerfile",
                "src/main/resources/application-dev.yml",
                "src/main/resources/application-prod.yml",
                "docker/pgadmin/servers.json"
            };
            long count = Arrays.stream(generatedFiles)
                    .filter(f -> new File(f).exists())
                    .count();
            log.info("  Generated config files: {}/{}", count, generatedFiles.length);
        } else if (isProd) {
            log.info("[PROD MODE] Using PostgreSQL");
        } else {
            log.info("[UNKNOWN MODE] Active profiles: {}", String.join(", ", activeProfiles));
        }

        log.info("================================================");
    }
}
