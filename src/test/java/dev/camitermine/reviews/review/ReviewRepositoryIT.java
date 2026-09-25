package dev.camitermine.reviews.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import dev.camitermine.reviews.TestcontainersConfiguration;

/**
 * Prueba las consultas JPQL contra un PostgreSQL real (Testcontainers) con el esquema de Flyway.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ReviewRepositoryIT {

    private static final long APP_ID = 2748340;
    private static final long OTHER_APP_ID = 1;

    @Autowired
    ReviewRepository repository;

    @BeforeEach
    void setUp() {
        repository.saveAll(List.of(
                review("1", APP_ID, true, ReviewCategory.POSITIVE),
                review("2", APP_ID, false, ReviewCategory.BUG),
                review("3", APP_ID, true, ReviewCategory.BUG),
                review("4", APP_ID, true, null),
                review("5", OTHER_APP_ID, false, ReviewCategory.BUG)));
    }

    @Test
    void searchAppliesOnlyTheGivenFilters() {
        var page = PageRequest.of(0, 10);

        assertThat(repository.search(APP_ID, null, null, page).getTotalElements()).isEqualTo(4);
        assertThat(repository.search(APP_ID, null, ReviewCategory.BUG, page).getTotalElements()).isEqualTo(2);
        assertThat(repository.search(APP_ID, false, ReviewCategory.BUG, page).getContent())
                .extracting(Review::getId).containsExactly("2");
    }

    @Test
    void countByCategoryIgnoresUnclassifiedAndOtherGames() {
        assertThat(repository.countByCategory(APP_ID)).containsExactlyInAnyOrder(
                new CategoryCount(ReviewCategory.POSITIVE, 1),
                new CategoryCount(ReviewCategory.BUG, 2));
    }

    @Test
    void clearCategoriesOnlyAffectsTheGivenGame() {
        assertThat(repository.clearCategories(APP_ID)).isEqualTo(4);
        assertThat(repository.findByAppIdAndCategoryIsNull(APP_ID, PageRequest.of(0, 10))).hasSize(4);
        assertThat(repository.findByAppIdAndCategoryIsNull(OTHER_APP_ID, PageRequest.of(0, 10))).isEmpty();
    }

    private static Review review(String id, long appId, boolean votedUp, ReviewCategory category) {
        Review review = new Review(id, appId, "english", "text " + id, votedUp, 60, Instant.parse("2026-01-01T00:00:00Z"));
        if (category != null) {
            review.classifyAs(category);
        }
        return review;
    }
}
