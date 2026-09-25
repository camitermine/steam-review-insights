package dev.camitermine.reviews;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SteamReviewInsightsApplication {

	public static void main(String[] args) {
		// Toda la app trabaja en UTC. Además evita que PostgreSQL rechace zonas horarias
		// con nombres viejos (por ejemplo "America/Buenos_Aires" en algunos Windows).
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(SteamReviewInsightsApplication.class, args);
	}

}
