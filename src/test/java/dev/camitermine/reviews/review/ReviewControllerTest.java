package dev.camitermine.reviews.review;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.camitermine.reviews.steam.SteamProperties;

@WebMvcTest(ReviewController.class)
@EnableConfigurationProperties(SteamProperties.class)
class ReviewControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReviewImportService importService;

    @MockitoBean
    ReviewRepository repository;

    @Test
    void statsCalculatesPositivePercent() throws Exception {
        when(repository.countByAppId(2748340)).thenReturn(8L);
        when(repository.countByAppIdAndVotedUp(2748340, true)).thenReturn(6L);

        mvc.perform(get("/api/reviews/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(8))
                .andExpect(jsonPath("$.negative").value(2))
                .andExpect(jsonPath("$.positivePercent").value(75.0));
    }

    @Test
    void importRejectsTooManyPages() throws Exception {
        mvc.perform(post("/api/reviews/import").param("maxPages", "500"))
                .andExpect(status().isBadRequest());
    }
}
