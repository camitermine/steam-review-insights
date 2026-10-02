package dev.camitermine.reviews.steam;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Una página de reseñas de IUserReviewsService/GetAppReviews.
 * Solo mapeamos los campos que usamos; el resto se ignora.
 * A diferencia del endpoint viejo (/appreviews), no hay campo "success": los errores llegan como status HTTP.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamReviewPage(String cursor, List<SteamReview> reviews) {

    static final SteamReviewPage EMPTY = new SteamReviewPage(null, List.of());

    /** La Web API de Steam envuelve todo en {"response": {...}}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope(SteamReviewPage response) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SteamReview(
            @JsonProperty("recommendationid") String id,
            Author author,
            String language,
            @JsonProperty("review") String text,
            @JsonProperty("timestamp_created") long timestampCreated,
            @JsonProperty("voted_up") boolean votedUp) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(
            @JsonProperty("steamid") String steamId,
            @JsonProperty("playtime_at_review") int playtimeAtReview) {
    }
}
