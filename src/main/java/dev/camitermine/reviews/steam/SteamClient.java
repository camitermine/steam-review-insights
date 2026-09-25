package dev.camitermine.reviews.steam;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP para la API pública de reseñas de Steam (no requiere API key).
 * Documentación: https://partner.steamgames.com/doc/store/getreviews
 */
@Component
public class SteamClient {

    static final int PAGE_SIZE = 100; // máximo que permite Steam

    private final RestClient restClient;

    public SteamClient(RestClient.Builder builder, SteamProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    /**
     * Trae una página de reseñas. Para la primera página se usa el cursor "*";
     * cada respuesta trae el cursor de la página siguiente.
     */
    public SteamReviewPage fetchPage(long appId, String cursor) {
        return restClient.get()
                .uri(uri -> uri.path("/appreviews/{appId}")
                        .queryParam("json", 1)
                        .queryParam("filter", "recent")
                        .queryParam("language", "all")
                        .queryParam("purchase_type", "all")
                        .queryParam("num_per_page", PAGE_SIZE)
                        // El cursor trae caracteres como "+" y "=", por eso va como variable: así se codifica bien.
                        .queryParam("cursor", "{cursor}")
                        .build(appId, cursor))
                .retrieve()
                .body(SteamReviewPage.class);
    }
}
