package dev.camitermine.reviews.digest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DigestService.class);

    // Discord admite hasta 10 embeds y 6.000 caracteres por mensaje: 3 + 3 reseñas de hasta
    // 900 caracteres, más el encabezado con el resumen, entran con margen.
    static final int HIGHLIGHTS_PER_CATEGORY = 3;
    static final int MAX_TEXT_CHARS = 900;
    /** Reseñas que se le pasan al resumen de IA. */
    static final int MAX_REVIEWS_TO_SUMMARIZE = 60;

    private final ReviewRepository repository;
    private final WeeklySummarizer summarizer;
    private final Clock clock;

    public DigestService(ReviewRepository repository, WeeklySummarizer summarizer, Clock clock) {
        this.repository = repository;
        this.summarizer = summarizer;
        this.clock = clock;
    }

    /**
     * @param text      texto de la reseña, recortado a {@link #MAX_TEXT_CHARS} si es muy largo
     * @param truncated true si se recortó; en ese caso conviene mostrar el link a Steam
     * @param steamUrl  link a la reseña en Steam, o null si todavía no conocemos al autor
     */
    public record Highlight(String id, boolean votedUp, Instant createdAt, double playtimeHours,
                            String text, boolean truncated, String steamUrl) {
    }

    /**
     * @param summary 2 o 3 frases generadas por IA con lo que más se repitió; vacío si no hay
     *                bugs ni pedidos de mejora, o si la API de Claude falló (el resumen sigue sin él)
     */
    public record Digest(int days, Instant from, Instant to, long total, long positive, long negative,
                         double positivePercent, Map<ReviewCategory, Long> byCategory, List<String> summary,
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
                summary(appId, from),
                highlights(appId, ReviewCategory.BUG, from),
                highlights(appId, ReviewCategory.FEATURE_REQUEST, from));
    }

    /** Resume los bugs y pedidos de mejora del período. Si la IA falla, el resumen semanal se manda igual. */
    private List<String> summary(long appId, Instant from) {
        var page = PageRequest.of(0, MAX_REVIEWS_TO_SUMMARIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Review> actionable = repository.findByAppIdAndCategoryInAndCreatedAtGreaterThanEqual(
                appId, List.of(ReviewCategory.BUG, ReviewCategory.FEATURE_REQUEST), from, page);
        if (actionable.isEmpty()) {
            return List.of();
        }
        try {
            return summarizer.summarize(actionable);
        } catch (RuntimeException e) {
            log.warn("No se pudo generar el resumen con IA; el resumen semanal sale sin él", e);
            return List.of();
        }
    }

    /** Las reseñas más recientes de la categoría. */
    private List<Highlight> highlights(long appId, ReviewCategory category, Instant from) {
        var page = PageRequest.of(0, HIGHLIGHTS_PER_CATEGORY, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findByAppIdAndCategoryAndCreatedAtGreaterThanEqual(appId, category, from, page).stream()
                .map(DigestService::toHighlight)
                .toList();
    }

    static Highlight toHighlight(Review r) {
        String text = cleanText(r.getText());
        boolean truncated = text.length() > MAX_TEXT_CHARS;
        if (truncated) {
            text = text.substring(0, MAX_TEXT_CHARS).strip() + "…";
        }
        double hours = Math.round(r.getPlaytimeAtReview() / 6.0) / 10.0; // minutos → horas, 1 decimal
        return new Highlight(r.getId(), r.isVotedUp(), r.getCreatedAt(), hours, text, truncated, r.getSteamUrl());
    }

    /** Mantiene los saltos de párrafo (se leen mejor en Discord) pero saca espacios y líneas vacías de más. */
    static String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }
}
