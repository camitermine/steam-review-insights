package dev.camitermine.reviews.classification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Valores de configuración que empiezan con "app.classifier" en application.properties.
 *
 * @param model        modelo de Claude a usar
 * @param batchSize    reseñas por llamada a la API (más grande = menos costo por reseña)
 * @param maxTextChars largo máximo de cada reseña que se envía; las muy largas se recortan para acotar el costo
 */
@ConfigurationProperties(prefix = "app.classifier")
public record ClassifierProperties(String model, int batchSize, int maxTextChars) {
}
