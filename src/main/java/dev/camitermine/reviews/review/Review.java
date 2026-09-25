package dev.camitermine.reviews.review;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    protected Review() {
        // requerido por JPA
    }

    public Review(String id, long appId, String language, String text, boolean votedUp,
                  int playtimeAtReview, Instant createdAt) {
        this.id = id;
        this.appId = appId;
        this.language = language;
        this.text = text;
        this.votedUp = votedUp;
        this.playtimeAtReview = playtimeAtReview;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public long getAppId() { return appId; }
    public String getLanguage() { return language; }
    public String getText() { return text; }
    public boolean isVotedUp() { return votedUp; }
    public int getPlaytimeAtReview() { return playtimeAtReview; }
    public Instant getCreatedAt() { return createdAt; }
}
