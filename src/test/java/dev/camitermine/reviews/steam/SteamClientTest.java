package dev.camitermine.reviews.steam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class SteamClientTest {

    private static final long APP_ID = 2748340;
    private static final String BASE_URL = "https://api.steampowered.com";

    /** Formato real de GetAppReviews: todo viene dentro de "response" y no hay campo "success". */
    private static final String PAGE_JSON = """
            {"response":{"query_summary":{"num_reviews":1},
              "reviews":[{"recommendationid":"236514443","author":{"playtime_at_review":203},
                          "language":"english","review":"Good.","timestamp_created":1790731395,"voted_up":true}],
              "cursor":"AoJ4m/jp9aADdqL9hQc=","total_matching":2366}}""";

    private MockRestServiceServer server;
    private SteamClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SteamClient(builder, new SteamProperties(BASE_URL, APP_ID, 2, Duration.ZERO));
    }

    @Test
    void callsGetAppReviewsWithInputJsonAndParsesTheEnvelope() {
        String expectedInput = SteamClient.buildInputJson(APP_ID, "*");
        server.expect(once(), requestTo(Matchers.startsWith(BASE_URL + "/IUserReviewsService/GetAppReviews/v1/")))
                .andExpect(method(HttpMethod.GET))
                // Se compara el JSON decodificado: la codificación exacta (p. ej. "*" vs "%2A") da igual.
                .andExpect(request -> assertThat(decodedInputJson(request.getURI())).isEqualTo(expectedInput))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));

        SteamReviewPage page = client.fetchPage(APP_ID, "*");

        assertThat(page.cursor()).isEqualTo("AoJ4m/jp9aADdqL9hQc=");
        assertThat(page.reviews()).singleElement().satisfies(r -> {
            assertThat(r.id()).isEqualTo("236514443");
            assertThat(r.text()).isEqualTo("Good.");
            assertThat(r.votedUp()).isTrue();
            assertThat(r.author().playtimeAtReview()).isEqualTo(203);
        });
        server.verify();
    }

    private static String decodedInputJson(URI uri) {
        String raw = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("input_json");
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    @Test
    void inputJsonUsesTheNewEnumValues() {
        assertThat(SteamClient.buildInputJson(APP_ID, "AoJ4m+x="))
                .isEqualTo("{\"appid\":2748340,\"filter\":1,\"languages\":[\"all\"],\"purchase_type\":1,"
                        + "\"num_per_page\":100,\"cursor\":\"AoJ4m+x=\"}");
    }

    @Test
    void retriesWhenRateLimitedAndThenSucceeds() {
        server.expect(times(2), requestTo(Matchers.startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(once(), requestTo(Matchers.startsWith(BASE_URL)))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));

        SteamReviewPage page = client.fetchPage(APP_ID, "*");

        assertThat(page.reviews()).hasSize(1);
        server.verify();
    }

    @Test
    void givesUpAfterMaxRetries() {
        server.expect(times(3), requestTo(Matchers.startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.fetchPage(APP_ID, "*"))
                .isInstanceOf(HttpClientErrorException.TooManyRequests.class);
        server.verify();
    }

    @Test
    void missingResponseBecomesAnEmptyPage() {
        server.expect(once(), requestTo(Matchers.startsWith(BASE_URL)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.fetchPage(APP_ID, "*").reviews()).isEmpty();
    }
}
