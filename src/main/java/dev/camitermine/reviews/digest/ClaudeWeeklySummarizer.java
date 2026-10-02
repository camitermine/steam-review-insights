package dev.camitermine.reviews.digest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.camitermine.reviews.classification.ClassifierProperties;
import dev.camitermine.reviews.review.Review;

/**
 * Pide a Claude 2 o 3 frases con lo que más se repitió en las reseñas de la semana.
 * Usa structured outputs para recibir una lista de frases, sin parsear texto libre.
 */
@Component
public class ClaudeWeeklySummarizer implements WeeklySummarizer {

    private static final Logger log = LoggerFactory.getLogger(ClaudeWeeklySummarizer.class);

    static final int MAX_REVIEWS = 60;
    static final int MAX_CHARS_PER_REVIEW = 1000;

    static final String SYSTEM_PROMPT = """
            You summarize a week of Steam reviews of Goblin Cleanup, a cooperative multiplayer game, \
            for its small development team. Reviews can be in any language; write in English.

            Return 2 or 3 items, one per topic, ordered by importance: the issues and requests that came \
            up most, so the team knows what to look at first. Each item is ONE sentence of at most 25 words. \
            Mention how many reviews raised the topic when it helps (e.g. "3 players report..."). Be concrete \
            (what breaks, where, when) and only state what the reviews actually say. If there is nothing \
            actionable, return a single item saying so.
            """;

    record Summary(@JsonPropertyDescription("2 or 3 items, one sentence of at most 25 words each")
                   List<String> sentences) {
    }

    private final AnthropicClient client;
    private final ClassifierProperties properties;

    public ClaudeWeeklySummarizer(ClassifierProperties properties) {
        // Lee ANTHROPIC_API_KEY de las variables de entorno.
        this.client = AnthropicOkHttpClient.fromEnv();
        this.properties = properties;
    }

    @Override
    public List<String> summarize(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        StructuredMessageCreateParams<Summary> params = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(1024L)
                .system(SYSTEM_PROMPT)
                .outputConfig(Summary.class)
                .addUserMessage(buildUserMessage(reviews))
                .build();

        var response = client.messages().create(params);
        log.info("Resumen semanal: {} reseñas, {} tokens de entrada, {} de salida", reviews.size(),
                response.usage().inputTokens(), response.usage().outputTokens());

        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .flatMap(typed -> typed.text().sentences().stream())
                .toList();
    }

    static String buildUserMessage(List<Review> reviews) {
        StringBuilder sb = new StringBuilder("Reviews from this week:\n\n");
        reviews.stream().limit(MAX_REVIEWS).forEach(r -> {
            String text = r.getText() == null ? "" : r.getText().strip();
            if (text.length() > MAX_CHARS_PER_REVIEW) {
                text = text.substring(0, MAX_CHARS_PER_REVIEW) + " [...]";
            }
            sb.append("<review category=\"").append(r.getCategory())
                    .append("\" recommended=\"").append(r.isVotedUp()).append("\">\n")
                    .append(text).append("\n</review>\n");
        });
        return sb.toString();
    }
}
