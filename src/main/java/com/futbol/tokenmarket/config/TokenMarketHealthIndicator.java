package com.futbol.tokenmarket.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class TokenMarketHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        File dataDir = new File("data");
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            return Health.down()
                    .withDetail("error", "Directorio de datos H2 no encontrado")
                    .withDetail("path", dataDir.getAbsolutePath())
                    .build();
        }
        return Health.up()
                .withDetail("dataDirectory", dataDir.getAbsolutePath())
                .withDetail("freeSpaceMB", dataDir.getFreeSpace() / (1024 * 1024))
                .build();
    }
}
