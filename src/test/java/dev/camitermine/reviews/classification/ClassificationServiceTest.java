package dev.camitermine.reviews.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.camitermine.reviews.review.Review;
import dev.camitermine.reviews.review.ReviewCategory;
import dev.camitermine.reviews.review.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ClassificationServiceTest {

    private static final long APP_ID = 2748340;

    @Mock
    ReviewClassifier classifier;

    @Mock
    ReviewRepository repository;

    ClassificationService service;

    @BeforeEach
    void setUp() {
        service = new ClassificationService(classifier, repository, new ClassifierProperties("test-model", 2, 2000));
    }

    @Test
    void emptyReviewsBecomeOtherWithoutCallingTheApi() {
        Review empty = review("1", "   ");
        when(repository.findByAppIdAndCategoryIsNull(any(Long.class), any())).thenReturn(List.of(empty));

        var run = service.classifyPending(APP_ID, 100);

        assertThat(empty.getCategory()).isEqualTo(ReviewCategory.OTHER);
        assertThat(run.empty()).isEqualTo(1);
        assertThat(run.apiCalls()).isZero();
        verify(classifier, never()).classify(anyList());
    }

    @Test
    void splitsReviewsIntoBatches() {
        Review a = review("a", "Crashes on load");
        Review b = review("b", "Love it");
        Review c = review("c", "Please add a map");
        when(repository.findByAppIdAndCategoryIsNull(any(Long.class), any())).thenReturn(List.of(a, b, c));
        when(classifier.classify(List.of(a, b)))
                .thenReturn(Map.of("a", ReviewCategory.BUG, "b", ReviewCategory.POSITIVE));
        when(classifier.classify(List.of(c))).thenReturn(Map.of("c", ReviewCategory.FEATURE_REQUEST));

        var run = service.classifyPending(APP_ID, 100);

        assertThat(run.apiCalls()).isEqualTo(2);
        assertThat(run.classified()).isEqualTo(3);
        assertThat(a.getCategory()).isEqualTo(ReviewCategory.BUG);
        assertThat(c.getCategory()).isEqualTo(ReviewCategory.FEATURE_REQUEST);
    }

    @Test
    void reviewsMissingFromTheResponseStayPending() {
        Review a = review("a", "Good game");
        Review b = review("b", "Bad netcode");
        when(repository.findByAppIdAndCategoryIsNull(any(Long.class), any())).thenReturn(List.of(a, b));
        when(classifier.classify(anyList())).thenReturn(Map.of("a", ReviewCategory.POSITIVE));

        var run = service.classifyPending(APP_ID, 100);

        assertThat(run.missing()).isEqualTo(1);
        assertThat(b.getCategory()).isNull();
        verify(repository, times(2)).saveAll(anyList());
    }

    private static Review review(String id, String text) {
        return new Review(id, APP_ID, "english", text, true, 60, Instant.EPOCH);
    }
}
