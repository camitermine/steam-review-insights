package dev.camitermine.reviews.steam;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP para IUserReviewsService/GetAppReviews de la Web API de Steam.
 * No requiere key: las llamadas anónimas comparten un rate limit más bajo y, si se supera, Steam responde 429.
 * Documentación: https://partner.steamgames.com/doc/webapi/IUserReviewsService
 *
 * Reemplaza al endpoint /appreviews de la tienda, que Steam desactiva el 22/10/2026.
 */
@Component
public class SteamClient {

    private static final Logger log = LoggerFactory.getLogger(SteamClient.class);

    static final int PAGE_SIZE = 100; // máximo que permite Steam

    // Valores de los enums de la API (ver documentación).
    private static final int FILTER_RECENT = 1;      // EUserReviewsAppReviewsFilter: más recientes primero
    private static final int PURCHASE_TYPE_ALL = 1;  // EUserReviewsPurchaseType: compradas en Steam o no

    private final RestClient restClient;
    private final int maxRetries;
    private final Duration retryBackoff;

    public SteamClient(RestClient.Builder builder, SteamProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.maxRetries = properties.maxRetries();
        this.retryBackoff = properties.retryBackoff() == null ? Duration.ZERO : properties.retryBackoff();
    }

    /**
     * Trae una página de reseñas. Para la primera página se usa el cursor "*";
     * cada respuesta trae el cursor de la página siguiente. Cuando no hay más, la lista viene vacía.
     */
    public SteamReviewPage fetchPage(long appId, String cursor) {
        String inputJson = buildInputJson(appId, cursor);
        for (int attempt = 0; ; attempt++) {
            try {
                SteamReviewPage.Envelope envelope = restClient.get()
                        .uri(uri -> uri.path("/IUserReviewsService/GetAppReviews/v1/")
                                // Va como variable para que el JSON (llaves, comillas, cursor con "+" y "=") se codifique bien.
                                .queryParam("input_json", "{inputJson}")
                                .build(inputJson))
                        .retrieve()
                        .body(SteamReviewPage.Envelope.class);
                return envelope == null || envelope.response() == null ? SteamReviewPage.EMPTY : envelope.response();
            } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException e) {
                if (attempt >= maxRetries) {
                    throw e;
                }
                Duration wait = retryBackoff.multipliedBy(1L << attempt);
                log.warn("Steam respondió {}; reintento {}/{} en {} ms",
                        e.getStatusCode().value(), attempt + 1, maxRetries, wait.toMillis());
                sleep(wait);
            }
        }
    }

    static String buildInputJson(long appId, String cursor) {
        return """
                {"appid":%d,"filter":%d,"languages":["all"],"purchase_type":%d,"num_per_page":%d,"cursor":"%s"}"""
                .formatted(appId, FILTER_RECENT, PURCHASE_TYPE_ALL, PAGE_SIZE, escapeJson(cursor));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido mientras esperaba para reintentar", e);
        }
    }
}
