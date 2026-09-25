# Steam Review Insights

REST API that imports the Steam reviews of a game and turns them into actionable feedback for the dev team.

Built around [Goblin Cleanup](https://store.steampowered.com/app/2748340/) (published by Team17), a multiplayer game I work on.

## Features

- Imports all reviews from Steam's public review API (cursor-based pagination, no API key needed)
- Idempotent imports: re-running never duplicates reviews
- Paginated listing with filters (positive / negative)
- Aggregated stats (total, positive, negative, % positive)

### Roadmap

- [ ] Classify reviews with an LLM (bug / feature request / positive)
- [ ] OpenAPI / Swagger documentation
- [ ] PostgreSQL + Docker Compose
- [ ] CI with GitHub Actions
- [ ] Weekly digest to Discord with n8n

## Tech stack

Java 21 · Spring Boot 4 · Spring Data JPA / Hibernate · H2 (dev) · JUnit 5 · Mockito · Maven

## Running locally

Requirements: Java 21.

```bash
./mvnw spring-boot:run
```

The API runs on `http://localhost:8081`.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/reviews/import?maxPages=30` | Imports up to `maxPages` × 100 reviews from Steam |
| `GET` | `/api/reviews?votedUp=false&page=0&size=20` | Lists reviews, newest first |
| `GET` | `/api/reviews/stats` | Totals and % positive |

Example:

```bash
curl -X POST "localhost:8081/api/reviews/import?maxPages=30"
# {"imported":2364,"skipped":0,"pages":24}

curl localhost:8081/api/reviews/stats
# {"appId":2748340,"total":2364,"positive":2077,"negative":287,"positivePercent":87.9}
```

## Tests

```bash
./mvnw test
```
