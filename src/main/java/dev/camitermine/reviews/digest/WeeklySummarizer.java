package dev.camitermine.reviews.digest;

import java.util.List;

import dev.camitermine.reviews.review.Review;

/**
 * Resume en pocas frases qué se repitió en las reseñas de un período.
 * Es una interfaz para poder testear el resumen semanal sin llamar a la API real.
 */
public interface WeeklySummarizer {

    /** @return 2 o 3 frases con los temas principales, o una lista vacía si no hay nada para resumir. */
    List<String> summarize(List<Review> reviews);
}
