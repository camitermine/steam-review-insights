package dev.camitermine.reviews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SteamReviewInsightsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SteamReviewInsightsApplication.class, args);
	}

}
