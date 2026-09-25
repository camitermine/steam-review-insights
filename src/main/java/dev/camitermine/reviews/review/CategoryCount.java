package dev.camitermine.reviews.review;

/** Resultado de agrupar reseñas por categoría. */
public record CategoryCount(ReviewCategory category, long count) {
}
