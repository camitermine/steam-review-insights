package dev.camitermine.reviews.review;

public enum ReviewCategory {
    /** Reporta un problema técnico: crash, freeze, desync, problemas de conexión, rendimiento. */
    BUG,
    /** Pide algo que el juego no tiene o sugiere un cambio. */
    FEATURE_REQUEST,
    /** Elogia el juego sin reportar problemas ni pedir cambios. */
    POSITIVE,
    /** Sin información accionable: reseñas vacías, memes, quejas de precio, etc. */
    OTHER
}
