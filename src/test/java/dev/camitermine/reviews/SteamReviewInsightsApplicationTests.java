package dev.camitermine.reviews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import dev.camitermine.reviews.classification.ReviewClassifier;
import dev.camitermine.reviews.digest.WeeklySummarizer;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SteamReviewInsightsApplicationTests {

	// Reemplaza al clasificador real: los tests no necesitan API key ni gastan créditos
	@MockitoBean
	ReviewClassifier classifier;

	@MockitoBean
	WeeklySummarizer summarizer;

	@Test
	void contextLoads() {
	}

}
