package dev.camitermine.reviews.digest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import dev.camitermine.reviews.review.CategoryCount;
import dev.camitermine.reviews.review.Review;
import dev.camitermine.reviews.review.ReviewCategory;
import dev.camitermine.reviews.review.ReviewRepository;

/**
 * Arma el resumen de las reseñas de los últimos N días. Lo consume el flujo de n8n
 * que manda el reporte semanal a Discord.
 */
@Service
public class DigestService {

    static final int HIGHLIGHTS_PER_CATEGORY = 5;
    static final int MAX_EXCERPT_CHARS = 300;

    private final ReviewRepository repository;
    private final Clock clock;

    public DigestService(ReviewRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public record Highlight(String id, String language, boolean votedUp, Instant createdAt, String excerpt) {
    }

    public record Digest(int days, Instant from, Instant to, long total, long positive, long negative,
                         double positivePercent, Map<ReviewCategory, Long> byCategory,
                         List<Highlight> topBugs, List<Highlight> topFeatureRequests) {
    }

    public Digest lastDays(long appId, int days) {
        Instant to = clock.instant();
        Instant from = to.minus(Duration.ofDays(days));

        long total = repository.countByAppIdAndCreatedAtGreaterThanEqual(appId, from);
        long positive = repository.countByAppIdAndVotedUpAndCreatedAtGreaterThanEqual(appId, true, from);
        double percent = total == 0 ? 0 : Math.round(positive * 1000.0 / total) / 10.0;

        Map<ReviewCategory, Long> byCategory = new EnumMap<>(ReviewCategory.class);
        for (ReviewCategory category : ReviewCategory.values()) {
            byCategory.put(category, 0L);
        }
        for (CategoryCount count : repository.countByCategorySince(appId, from)) {
            byCategory.put(count.category(), count.count());
        }

        return new Digest(days, from, to, total, positive, total - positive, percent, byCategory,
                highlights(appId, ReviewCategory.BUG, from),
                highlights(appId, ReviewCategory.FEATURE_REQUEST, from));
    }

    /** Las reseñas más recientes de la categoría, con el texto recortado para que entren en un mensaje. */
    private List<Highlight> highlights(long appId, ReviewCategory category, Instant from) {
        var page = PageRequest.of(0, HIGHLIGHTS_PER_CATEGORY, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findByAppIdAndCategoryAndCreatedAtGreaterThanEqual(appId, category, from, page).stream()
                .map(r -> new Highlight(r.getId(), r.getLanguage(), r.isVotedUp(), r.getCreatedAt(), excerpt(r)))
                .toList();
    }

    static String excerpt(Review review) {
        String text = review.getText() == null ? "" : review.getText().replaceAll("\\s+", " ").strip();
        return text.length() <= MAX_EXCERPT_CHARS ? text : text.substring(0, MAX_EXCERPT_CHARS) + "…";
    }
}
