package dev.camitermine.reviews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import dev.camitermine.reviews.classification.ReviewClassifier;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SteamReviewInsightsApplicationTests {

	// Reemplaza al clasificador real: los tests no necesitan API key ni gastan créditos
	@MockitoBean
	ReviewClassifier classifier;

	@Test
	void contextLoads() {
	}

}
