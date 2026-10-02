package dev.camitermine.reviews.steam;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Valores de configuración que empiezan con "app.steam" en application.properties.
 *
 * @param baseUrl      base de la Web API de Steam
 * @param appId        juego del que se importan las reseñas
 * @param maxRetries   reintentos ante 429 (rate limit) o errores 5xx de Steam
 * @param retryBackoff espera antes del primer reintento; se duplica en cada intento
 */
@ConfigurationProperties(prefix = "app.steam")
public record SteamProperties(String baseUrl, long appId, int maxRetries, Duration retryBackoff) {
}
