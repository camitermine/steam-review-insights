package dev.camitermine.reviews.review;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    public record ImportResult(int imported, int skipped, int pages) {
    }

    @Transactional
    public ImportResult importReviews(long appId, int maxPages) {
        String cursor = "*";
        int imported = 0;
        int skipped = 0;
        int pages = 0;

        while (pages < maxPages) {
            SteamReviewPage page = steamClient.fetchPage(appId, cursor);
            if (page == null || page.success() != 1 || page.reviews() == null || page.reviews().isEmpty()) {
                break;
            }
            pages++;

            List<Review> toSave = new ArrayList<>();
            for (SteamReview steamReview : page.reviews()) {
                if (repository.existsById(steamReview.id())) {
                    skipped++;
                } else {
                    toSave.add(toEntity(appId, steamReview));
                }
            }
            repository.saveAll(toSave);
            imported += toSave.size();

            // Steam devuelve el mismo cursor cuando ya no hay más páginas.
            if (page.cursor() == null || page.cursor().equals(cursor)) {
                break;
            }
            cursor = page.cursor();
        }

        log.info("Importación de app {}: {} nuevas, {} repetidas, {} páginas", appId, imported, skipped, pages);
        return new ImportResult(imported, skipped, pages);
    }

    private static Review toEntity(long appId, SteamReview r) {
        int playtime = r.author() != null ? r.author().playtimeAtReview() : 0;
        return new Review(r.id(), appId, r.language(), r.text(), r.votedUp(), playtime,
                Instant.ofEpochSecond(r.timestampCreated()));
    }
}
