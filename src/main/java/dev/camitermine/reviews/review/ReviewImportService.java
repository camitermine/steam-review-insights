package dev.camitermine.reviews.review;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.camitermine.reviews.steam.SteamClient;
import dev.camitermine.reviews.steam.SteamReviewPage;
import dev.camitermine.reviews.steam.SteamReviewPage.SteamReview;

/**
 * Recorre las páginas de reseñas de Steam y guarda las que todavía no están en la base.
 */
@Service
public class ReviewImportService {

    private static final Logger log = LoggerFactory.getLogger(ReviewImportService.class);

    private final SteamClient steamClient;
    private final ReviewRepository repository;

    public ReviewImportService(SteamClient steamClient, ReviewRepository repository) {
        this.steamClient = steamClient;
        this.repository = repository;
    }

    public record ImportResult(int imported, int skipped, int updated, int pages) {
    }

    @Transactional
    public ImportResult importReviews(long appId, int maxPages) {
        String cursor = "*";
        int imported = 0;
        int skipped = 0;
        int updated = 0;
        int pages = 0;

        while (pages < maxPages) {
            SteamReviewPage page = steamClient.fetchPage(appId, cursor);
            if (page == null || page.reviews() == null || page.reviews().isEmpty()) {
                break;
            }
            pages++;

            // Una sola consulta por página para saber cuáles ya teníamos.
            Map<String, Review> existing = repository.findAllById(
                            page.reviews().stream().map(SteamReview::id).toList()).stream()
                    .collect(Collectors.toMap(Review::getId, Function.identity()));

            List<Review> toSave = new ArrayList<>();
            for (SteamReview steamReview : page.reviews()) {
                Review stored = existing.get(steamReview.id());
                if (stored == null) {
                    toSave.add(toEntity(appId, steamReview));
                    imported++;
                } else {
                    skipped++;
                    // Reseñas importadas antes de guardar el autor: lo completamos para poder linkearlas.
                    if (steamReview.author() != null && stored.backfillAuthor(steamReview.author().steamId())) {
                        toSave.add(stored);
                        updated++;
                    }
                }
            }
            repository.saveAll(toSave);

            // Por seguridad: si Steam repite el cursor, no hay más páginas.
            if (page.cursor() == null || page.cursor().equals(cursor)) {
                break;
            }
            cursor = page.cursor();
        }

        log.info("Importación de app {}: {} nuevas, {} repetidas ({} con autor completado), {} páginas",
                appId, imported, skipped, updated, pages);
        return new ImportResult(imported, skipped, updated, pages);
    }

    private static Review toEntity(long appId, SteamReview r) {
        int playtime = r.author() != null ? r.author().playtimeAtReview() : 0;
        String steamId = r.author() != null ? r.author().steamId() : null;
        return new Review(r.id(), appId, r.language(), r.text(), r.votedUp(), playtime,
                Instant.ofEpochSecond(r.timestampCreated()), steamId);
    }
}
