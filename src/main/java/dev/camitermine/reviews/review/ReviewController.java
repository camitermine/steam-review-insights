package dev.camitermine.reviews.review;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.camitermine.reviews.classification.ClassificationService;
import dev.camitermine.reviews.classification.ClassificationService.ClassificationRun;
import dev.camitermine.reviews.review.ReviewImportService.ImportResult;
import dev.camitermine.reviews.steam.SteamProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/reviews")
@Validated
@Tag(name = "Reviews", description = "Import, classify and query Steam reviews")
public class ReviewController {

    private final ReviewImportService importService;
    private final ClassificationService classificationService;
    private final ReviewRepository repository;
    private final SteamProperties steamProperties;

    public ReviewController(ReviewImportService importService, ClassificationService classificationService,
                            ReviewRepository repository, SteamProperties steamProperties) {
        this.importService = importService;
        this.classificationService = classificationService;
        this.repository = repository;
        this.steamProperties = steamProperties;
    }

    /** Lo que devuelve la API: no exponemos la entidad JPA directamente. */
    public record ReviewResponse(String id, String language, String text, boolean votedUp,
                                 int playtimeAtReviewMinutes, Instant createdAt, ReviewCategory category) {
        static ReviewResponse from(Review r) {
            return new ReviewResponse(r.getId(), r.getLanguage(), r.getText(), r.isVotedUp(),
                    r.getPlaytimeAtReview(), r.getCreatedAt(), r.getCategory());
        }
    }

    public record StatsResponse(long appId, long total, long positive, long negative, double positivePercent,
                                Map<ReviewCategory, Long> byCategory, long unclassified) {
    }

    @Operation(summary = "Import reviews from Steam",
            description = "Fetches up to maxPages × 100 of the newest reviews. Reviews already stored are skipped.")
    @PostMapping("/import")
    public ImportResult importReviews(@RequestParam(defaultValue = "5") @Min(1) @Max(100) int maxPages) {
        return importService.importReviews(steamProperties.appId(), maxPages);
    }

    @Operation(summary = "Classify pending reviews with Claude",
            description = "Classifies up to `limit` reviews that have no category yet. Empty reviews become OTHER without calling the API.")
    @PostMapping("/classify")
    public ClassificationRun classify(@RequestParam(defaultValue = "100") @Min(1) @Max(5000) int limit) {
        return classificationService.classifyPending(steamProperties.appId(), limit);
    }

    public record ClearResult(int cleared) {
    }

    @Operation(summary = "Clear all classifications",
            description = "Sets every category back to null, e.g. to re-run classification after changing the prompt.")
    @DeleteMapping("/classifications")
    public ClearResult clearClassifications() {
        return new ClearResult(repository.clearCategories(steamProperties.appId()));
    }

    @Operation(summary = "List reviews", description = "Newest first. All filters are optional.")
    @GetMapping
    public PagedModel<ReviewResponse> list(@RequestParam(required = false) Boolean votedUp,
                                           @RequestParam(required = false) ReviewCategory category,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var reviews = repository.search(steamProperties.appId(), votedUp, category, pageable);
        return new PagedModel<>(reviews.map(ReviewResponse::from));
    }

    /** Columnas separadas por ";" y con BOM UTF-8, para que Excel en español las abra bien de entrada. */
    @Operation(summary = "Export all reviews as CSV",
            description = "Semicolon-separated, UTF-8 with BOM so Excel opens it correctly. Newest first.")
    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv() {
        StringBuilder csv = new StringBuilder("﻿"); // BOM: Excel detecta UTF-8 (acentos, emojis)
        csv.append("fecha;voto;categoria;horas_jugadas;resena;link_steam;id\n");
        for (Review r : repository.findByAppId(steamProperties.appId(), Sort.by(Sort.Direction.DESC, "createdAt"))) {
            csv.append(CSV_DATE.format(r.getCreatedAt())).append(';')
                    .append(r.isVotedUp() ? "Positiva" : "Negativa").append(';')
                    .append(r.getCategory() == null ? "SIN CLASIFICAR" : r.getCategory().name()).append(';')
                    .append(String.format(Locale.ROOT, "%.1f", r.getPlaytimeAtReview() / 60.0)).append(';')
                    .append(csvField(r.getText())).append(';')
                    .append(r.getSteamUrl() == null ? "" : r.getSteamUrl()).append(';')
                    .append(r.getId()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reviews.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString());
    }

    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    /** Entre comillas, duplicando las comillas internas, para que los ";" y saltos de línea del texto no rompan el CSV. */
    static String csvField(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }

    @Operation(summary = "Review statistics", description = "Totals, % positive and counts per category.")
    @GetMapping("/stats")
    public StatsResponse stats() {
        long appId = steamProperties.appId();
        long total = repository.countByAppId(appId);
        long positive = repository.countByAppIdAndVotedUp(appId, true);
        double percent = total == 0 ? 0 : Math.round(positive * 1000.0 / total) / 10.0;

        Map<ReviewCategory, Long> byCategory = new EnumMap<>(ReviewCategory.class);
        for (ReviewCategory category : ReviewCategory.values()) {
            byCategory.put(category, 0L);
        }
        long classified = 0;
        for (CategoryCount count : repository.countByCategory(appId)) {
            byCategory.put(count.category(), count.count());
            classified += count.count();
        }
        return new StatsResponse(appId, total, positive, total - positive, percent, byCategory, total - classified);
    }
}
