package dev.camitermine.reviews;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.anthropic.errors.AnthropicException;

import jakarta.validation.ConstraintViolationException;

/**
 * Traduce excepciones a respuestas HTTP con el formato estándar ProblemDetail (RFC 9457).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Parámetros inválidos: es un error del cliente (400), no del servidor (500). */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleValidation(ConstraintViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Falló la API de Claude (sin créditos, key inválida, rate limit): 502 = falló un servicio externo. */
    @ExceptionHandler(AnthropicException.class)
    ProblemDetail handleClaudeError(AnthropicException ex) {
        log.error("Error llamando a la API de Claude", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Claude API error: " + ex.getMessage() + ". Reviews classified before the error were saved.");
    }
}
