package dev.camitermine.reviews.classification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.camitermine.reviews.review.Review;
import dev.camitermine.reviews.review.ReviewCategory;

/**
 * Clasifica reseñas con Claude usando structured outputs: la API garantiza que la respuesta
 * cumple el esquema JSON de {@link Result}, así que no hay que parsear texto libre.
 */
@Component
public class ClaudeReviewClassifier implements ReviewClassifier {

    private static final Logger log = LoggerFactory.getLogger(ClaudeReviewClassifier.class);

    static final String SYSTEM_PROMPT = """
            You classify Steam reviews of Goblin Cleanup, a cooperative multiplayer game about \
            cleaning up a dungeon after adventurers pass through. Reviews can be in any language.

            Assign exactly one category to each review:
            - BUG: reports something that malfunctions (crashes, freezes, desyncs, connection or lobby \
            issues, lost progress, poor performance), even if the review is positive overall.
            - FEATURE_REQUEST: asks for something the game lacks or suggests a change (more levels, more \
            settings, balance or design changes), and reports nothing malfunctioning. Asking for more \
            options is a request, not a bug.
            - POSITIVE: praises or recommends the game without reporting problems or requesting changes. \
            Short praise such as "good game", "is good" or "yes" counts as POSITIVE.
            - OTHER: no clear opinion or nothing actionable for the developers (jokes, memes, role-play, \
            off-topic text, complaints about price).

            If a review fits more than one category, choose the first one in the order above.
            Return one label for every review id you receive.
            """;

    record Label(String id, ReviewCategory category) {
    }

    record Result(@JsonPropertyDescription("One label per review") List<Label> labels) {
    }

    private final AnthropicClient client;
    private final ClassifierProperties properties;

    public ClaudeReviewClassifier(ClassifierProperties properties) {
        // Lee ANTHROPIC_API_KEY de las variables de entorno.
        this.client = AnthropicOkHttpClient.fromEnv();
        this.properties = properties;
    }

    @Override
    public Map<String, ReviewCategory> classify(List<Review> reviews) {
        StructuredMessageCreateParams<Result> params = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(4096L)
                .system(SYSTEM_PROMPT)
                .outputConfig(Result.class)
                .addUserMessage(buildUserMessage(reviews))
                .build();

        var response = client.messages().create(params);
        log.info("Claude: {} reseñas, {} tokens de entrada, {} de salida", reviews.size(),
                response.usage().inputTokens(), response.usage().outputTokens());

        Map<String, ReviewCategory> categories = new HashMap<>();
        response.content().stream()
                .flatMap(block -> block.text().stream())
                .flatMap(typed -> typed.text().labels().stream())
                .forEach(label -> categories.put(label.id(), label.category()));
        return categories;
    }

    String buildUserMessage(List<Review> reviews) {
        StringBuilder sb = new StringBuilder("Classify these reviews:\n\n");
        for (Review review : reviews) {
            String text = review.getText().strip();
            if (text.length() > properties.maxTextChars()) {
                text = text.substring(0, properties.maxTextChars()) + " [...]";
            }
            sb.append("<review id=\"").append(review.getId())
                    .append("\" recommended=\"").append(review.isVotedUp()).append("\">\n")
                    .append(text)
                    .append("\n</review>\n");
        }
        return sb.toString();
    }
}
