package dev.camitermine.reviews.review;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
public class Review {

    /** Usamos el recommendationid de Steam como clave: así nunca guardamos la misma reseña dos veces. */
    @Id
    private String id;

    @Column(nullable = false)
    private long appId;

    private String language;

    @Column(columnDefinition = "TEXT")
    private String text;

    private boolean votedUp;

    /** Minutos jugados cuando se escribió la reseña. */
    private int playtimeAtReview;

    private Instant createdAt;

    /** SteamID64 del autor. Null en reseñas importadas antes de la migración V2. */
    private String authorSteamId;

    /** Null hasta que la reseña se clasifica. */
    @Enumerated(EnumType.STRING)
    private ReviewCategory category;

    protected Review() {
        // requerido por JPA
    }

    public Review(String id, long appId, String language, String text, boolean votedUp,
                  int playtimeAtReview, Instant createdAt) {
        this(id, appId, language, text, votedUp, playtimeAtReview, createdAt, null);
    }

    public Review(String id, long appId, String language, String text, boolean votedUp,
                  int playtimeAtReview, Instant createdAt, String authorSteamId) {
        this.id = id;
        this.appId = appId;
        this.language = language;
        this.text = text;
        this.votedUp = votedUp;
        this.playtimeAtReview = playtimeAtReview;
        this.createdAt = createdAt;
        this.authorSteamId = authorSteamId;
    }

    public String getId() { return id; }
    public long getAppId() { return appId; }
    public String getLanguage() { return language; }
    public String getText() { return text; }
    public boolean isVotedUp() { return votedUp; }
    public int getPlaytimeAtReview() { return playtimeAtReview; }
    public Instant getCreatedAt() { return createdAt; }
    public ReviewCategory getCategory() { return category; }
    public String getAuthorSteamId() { return authorSteamId; }

    /** Link a la reseña en la comunidad de Steam, o null si no conocemos al autor. */
    public String getSteamUrl() {
        return authorSteamId == null ? null
                : "https://steamcommunity.com/profiles/" + authorSteamId + "/recommended/" + appId + "/";
    }

    /** Completa el autor en reseñas viejas. Devuelve true si cambió algo. */
    public boolean backfillAuthor(String steamId) {
        if (authorSteamId != null || steamId == null) {
            return false;
        }
        authorSteamId = steamId;
        return true;
    }

    public void classifyAs(ReviewCategory category) {
        this.category = category;
    }
}
