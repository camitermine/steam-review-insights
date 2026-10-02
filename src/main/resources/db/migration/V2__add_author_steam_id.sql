-- SteamID del autor: permite armar el link a la reseña en Steam.
-- Las reseñas importadas antes de esta migración lo completan en la próxima importación.
ALTER TABLE reviews ADD COLUMN author_steam_id VARCHAR(32);
