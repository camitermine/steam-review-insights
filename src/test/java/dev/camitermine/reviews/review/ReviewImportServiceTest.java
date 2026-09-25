package dev.camitermine.reviews.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.camitermine.reviews.steam.SteamClient;
import dev.camitermine.reviews.steam.SteamReviewPage;
import dev.camitermine.reviews.steam.SteamReviewPage.Author;
import dev.camitermine.reviews.steam.SteamReviewPage.SteamReview;

@ExtendWith(MockitoExtension.class)
class ReviewImportServiceTest {

    private static final long APP_ID = 2748340;

    @Mock
    SteamClient steamClient;

    @Mock
    ReviewRepository repository;

    @InjectMocks
    ReviewImportService service;

    @Test
    void importsNewReviewsAndSkipsExistingOnes() {
        var page = new SteamReviewPage(1, "next", List.of(review("1"), review("2")));
        when(steamClient.fetchPage(APP_ID, "*")).thenReturn(page);
        when(steamClient.fetchPage(APP_ID, "next")).thenReturn(new SteamReviewPage(1, "next", List.of()));
        when(repository.existsById("1")).thenReturn(true);
        when(repository.existsById("2")).thenReturn(false);

        var result = service.importReviews(APP_ID, 10);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.pages()).isEqualTo(1);
    }

    @Test
    void stopsAtMaxPages() {
        when(steamClient.fetchPage(APP_ID, "*")).thenReturn(new SteamReviewPage(1, "a", List.of(review("1"))));

        var result = service.importReviews(APP_ID, 1);

        assertThat(result.pages()).isEqualTo(1);
        verify(steamClient, never()).fetchPage(APP_ID, "a");
    }

    @Test
    void stopsWhenSteamFails() {
        when(steamClient.fetchPage(APP_ID, "*")).thenReturn(new SteamReviewPage(0, null, null));

        var result = service.importReviews(APP_ID, 10);

        assertThat(result.imported()).isZero();
        verify(repository, never()).saveAll(anyList());
    }

    private static SteamReview review(String id) {
        return new SteamReview(id, new Author(120), "english", "Fun game", 1_700_000_000L, true);
    }
}
