package dev.camitermine.reviews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import dev.camitermine.reviews.classification.ReviewClassifier;

// Base en memoria para que los tests no toquen ./data
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb")
class SteamReviewInsightsApplicationTests {

	// Reemplaza al clasificador real: los tests no necesitan API key ni gastan créditos
	@MockitoBean
	ReviewClassifier classifier;

	@Test
	void contextLoads() {
	}

}
