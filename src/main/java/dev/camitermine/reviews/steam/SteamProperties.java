package dev.camitermine.reviews.steam;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Valores de configuración que empiezan con "app.steam" en application.properties.
 */
@ConfigurationProperties(prefix = "app.steam")
public record SteamProperties(String baseUrl, long appId) {
}
