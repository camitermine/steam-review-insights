package dev.camitermine.reviews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Base en memoria para que los tests no toquen ./data
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb")
class SteamReviewInsightsApplicationTests {

	@Test
	void contextLoads() {
	}

}
