package dev.camitermine.reviews.digest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.convention.TestBean;

import dev.camitermine.reviews.TestcontainersConfiguration;
import dev.camitermine.reviews.review.Review;
import dev.camitermine.reviews.review.ReviewCategory;
import dev.camitermine.reviews.review.ReviewRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, DigestService.class})
class DigestServiceIT {

    private static final long APP_ID = 2748340;
    private static final Instant NOW = Instant.parse("2026-09-28T13:00:00Z");

    /** Reemplaza el reloj real por uno fijo: "ahora" siempre es NOW. */
    @TestBean
    Clock clock;

    static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Autowired
    ReviewRepository repository;

    @Autowired
    DigestService digestService;

    @BeforeEach
    void setUp() {
        repository.saveAll(List.of(
                review("recent-bug", daysAgo(1), false, ReviewCategory.BUG, "Crashes   when\nhosting"),
                review("recent-feature", daysAgo(3), true, ReviewCategory.FEATURE_REQUEST, "Add more levels"),
                review("recent-positive", daysAgo(6), true, ReviewCategory.POSITIVE, "Great"),
                review("old-bug", daysAgo(10), false, ReviewCategory.BUG, "Old crash")));
    }

    @Test
    void onlyCountsReviewsInsideTheWindow() {
        var digest = digestService.lastDays(APP_ID, 7);

        assertThat(digest.total()).isEqualTo(3);
        assertThat(digest.positive()).isEqualTo(2);
        assertThat(digest.positivePercent()).isEqualTo(66.7);
        assertThat(digest.byCategory().get(ReviewCategory.BUG)).isEqualTo(1);
        assertThat(digest.byCategory().get(ReviewCategory.OTHER)).isZero();
        assertThat(digest.topBugs()).extracting(DigestService.Highlight::id).containsExactly("recent-bug");
    }

    @Test
    void excerptsCollapseWhitespace() {
        var digest = digestService.lastDays(APP_ID, 7);

        assertThat(digest.topBugs().getFirst().excerpt()).isEqualTo("Crashes when hosting");
    }

    @Test
    void emptyWindowReturnsZeros() {
        repository.deleteAll();

        var digest = digestService.lastDays(APP_ID, 7);

        assertThat(digest.total()).isZero();
        assertThat(digest.positivePercent()).isZero();
        assertThat(digest.topBugs()).isEmpty();
    }

    private static Instant daysAgo(int days) {
        return NOW.minus(Duration.ofDays(days));
    }

    private static Review review(String id, Instant createdAt, boolean votedUp, ReviewCategory category, String text) {
        Review review = new Review(id, APP_ID, "english", text, votedUp, 60, createdAt);
        review.classifyAs(category);
        return review;
    }
}
