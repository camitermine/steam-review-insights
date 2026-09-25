package dev.camitermine.reviews.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import dev.camitermine.reviews.review.Review;
import dev.camitermine.reviews.review.ReviewCategory;
import dev.camitermine.reviews.review.ReviewRepository;

/**
 * Clasifica las reseñas que todavía no tienen categoría, en grupos de {@code batchSize}.
 * Cada grupo se guarda apenas se clasifica: si la API falla a mitad de camino, lo ya
 * clasificado no se pierde y la próxima ejecución sigue desde donde quedó.
 */
@Service
public class ClassificationService {

    private static final Logger log = LoggerFactory.getLogger(ClassificationService.class);

    private final ReviewClassifier classifier;
    private final ReviewRepository repository;
    private final ClassifierProperties properties;

    public ClassificationService(ReviewClassifier classifier, ReviewRepository repository,
                                 ClassifierProperties properties) {
        this.classifier = classifier;
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * @param classified reseñas clasificadas por el modelo
     * @param empty      reseñas sin texto, marcadas como OTHER sin llamar a la API
     * @param missing    reseñas que el modelo no devolvió; quedan pendientes para la próxima ejecución
     * @param apiCalls   llamadas hechas a la API
     */
    public record ClassificationRun(int classified, int empty, int missing, int apiCalls) {
    }

    public ClassificationRun classifyPending(long appId, int limit) {
        List<Review> pending = repository.findByAppIdAndCategoryIsNull(appId,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Review> withText = new ArrayList<>();
        List<Review> empty = new ArrayList<>();
        for (Review review : pending) {
            if (review.getText() == null || review.getText().isBlank()) {
                review.classifyAs(ReviewCategory.OTHER);
                empty.add(review);
            } else {
                withText.add(review);
            }
        }
        repository.saveAll(empty);

        int classified = 0;
        int missing = 0;
        int apiCalls = 0;
        for (int start = 0; start < withText.size(); start += properties.batchSize()) {
            List<Review> batch = withText.subList(start, Math.min(start + properties.batchSize(), withText.size()));
            Map<String, ReviewCategory> categories = classifier.classify(batch);
            apiCalls++;

            List<Review> toSave = new ArrayList<>();
            for (Review review : batch) {
                ReviewCategory category = categories.get(review.getId());
                if (category == null) {
                    missing++;
                } else {
                    review.classifyAs(category);
                    toSave.add(review);
                }
            }
            repository.saveAll(toSave);
            classified += toSave.size();
        }

        log.info("Clasificación de app {}: {} clasificadas, {} vacías, {} sin respuesta, {} llamadas",
                appId, classified, empty.size(), missing, apiCalls);
        return new ClassificationRun(classified, empty.size(), missing, apiCalls);
    }
}
