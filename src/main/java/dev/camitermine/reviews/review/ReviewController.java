package dev.camitermine.reviews.review;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.camitermine.reviews.review.ReviewImportService.ImportResult;
import dev.camitermine.reviews.steam.SteamProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/reviews")
@Validated
public class ReviewController {

    private final ReviewImportService importService;
    private final ReviewRepository repository;
    private final SteamProperties steamProperties;

    public ReviewController(ReviewImportService importService, ReviewRepository repository,
                            SteamProperties steamProperties) {
        this.importService = importService;
        this.repository = repository;
        this.steamProperties = steamProperties;
    }

    /** Lo que devuelve la API: no exponemos la entidad JPA directamente. */
    public record ReviewResponse(String id, String language, String text, boolean votedUp,
                                 int playtimeAtReviewMinutes, Instant createdAt) {
        static ReviewResponse from(Review r) {
            return new ReviewResponse(r.getId(), r.getLanguage(), r.getText(), r.isVotedUp(),
                    r.getPlaytimeAtReview(), r.getCreatedAt());
        }
    }

    public record StatsResponse(long appId, long total, long positive, long negative, double positivePercent) {
    }

    /** POST /api/reviews/import?maxPages=5 → importa hasta 5 páginas (500 reseñas). */
    @PostMapping("/import")
    public ImportResult importReviews(@RequestParam(defaultValue = "5") @Min(1) @Max(100) int maxPages) {
        return importService.importReviews(steamProperties.appId(), maxPages);
    }

    /** GET /api/reviews?votedUp=false&page=0&size=20 → lista paginada, las más recientes primero. */
    @GetMapping
    public PagedModel<ReviewResponse> list(@RequestParam(required = false) Boolean votedUp,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        long appId = steamProperties.appId();
        Page<Review> reviews = votedUp == null
                ? repository.findByAppId(appId, pageable)
                : repository.findByAppIdAndVotedUp(appId, votedUp, pageable);
        return new PagedModel<>(reviews.map(ReviewResponse::from));
    }

    /** GET /api/reviews/stats → totales y porcentaje positivo. */
    @GetMapping("/stats")
    public StatsResponse stats() {
        long appId = steamProperties.appId();
        long total = repository.countByAppId(appId);
        long positive = repository.countByAppIdAndVotedUp(appId, true);
        double percent = total == 0 ? 0 : Math.round(positive * 1000.0 / total) / 10.0;
        return new StatsResponse(appId, total, positive, total - positive, percent);
    }
}
