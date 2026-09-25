package dev.camitermine.reviews.steam;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Una página de la respuesta de /appreviews/{appId}?json=1.
 * Solo mapeamos los campos que usamos; el resto se ignora.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamReviewPage(int success, String cursor, List<SteamReview> reviews) {

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
    public record Author(@JsonProperty("playtime_at_review") int playtimeAtReview) {
    }
}
