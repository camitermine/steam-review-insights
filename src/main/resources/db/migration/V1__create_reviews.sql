CREATE TABLE reviews (
    id                 VARCHAR(32)  PRIMARY KEY, -- recommendationid de Steam
    app_id             BIGINT       NOT NULL,
    language           VARCHAR(32),
    text               TEXT,
    voted_up           BOOLEAN      NOT NULL,
    playtime_at_review INTEGER      NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE,
    category           VARCHAR(32)  -- null hasta que se clasifica
);

-- Las consultas siempre filtran por juego y por categoría.
CREATE INDEX idx_reviews_app_category ON reviews (app_id, category);
CREATE INDEX idx_reviews_app_created_at ON reviews (app_id, created_at DESC);
