package dev.camitermine.reviews.classification;

import java.util.List;
import java.util.Map;

import dev.camitermine.reviews.review.Review;
import dev.camitermine.reviews.review.ReviewCategory;

/**
 * Clasifica un grupo de reseñas. Es una interfaz para poder testear el servicio
 * sin llamar a la API real (y para poder cambiar de proveedor sin tocar el resto).
 */
public interface ReviewClassifier {

    /**
     * @return categoría por id de reseña. Las reseñas que el modelo no devolvió no aparecen en el mapa.
     */
    Map<String, ReviewCategory> classify(List<Review> reviews);
}
