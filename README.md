# Steam Review Insights

![CI](https://github.com/camitermine/steam-review-insights/actions/workflows/ci.yml/badge.svg)

REST API that imports the Steam reviews of a game and uses Claude to turn them into actionable feedback for the dev team: which reviews report bugs, which ask for features, and which are just praise.

Built around [Goblin Cleanup](https://store.steampowered.com/app/2748340/) (published by Team17), a multiplayer game I work on.

## Features

- Imports all reviews from Steam's public review API (cursor-based pagination, no API key needed)
- Idempotent imports: re-running never duplicates reviews
- **LLM classification** with Claude Haiku 4.5 into `BUG`, `FEATURE_REQUEST`, `POSITIVE` or `OTHER`, in any language
  - Structured outputs: the API guarantees the response matches a JSON schema, so there is no free-text parsing
  - 20 reviews per request to cut cost (~$0.005 per request)
  - Empty reviews are classified without calling the API
  - Progress is saved batch by batch: if the API fails, the next run resumes where it stopped
- Paginated listing with filters (positive / negative, category)
- Aggregated stats (% positive, counts per category)
- OpenAPI / Swagger documentation
- PostgreSQL with versioned schema migrations (Flyway)
- Dockerized: multi-stage image running as a non-root user, one-command setup with Docker Compose
- Integration tests against a real PostgreSQL with Testcontainers, run on every push by GitHub Actions

### Roadmap

- [x] Classify reviews with an LLM
- [x] OpenAPI / Swagger documentation
- [x] PostgreSQL + Docker Compose
- [x] CI with GitHub Actions
- [ ] Weekly digest to Discord with n8n

## Tech stack

Java 21 · Spring Boot 4 · Spring Data JPA / Hibernate · PostgreSQL 17 · Flyway · Anthropic Java SDK (Claude Haiku 4.5) · springdoc-openapi · Docker / Docker Compose · JUnit 5 · Mockito · Testcontainers · GitHub Actions · Maven

## Running locally

Requirements: Docker, and an [Anthropic API key](https://console.anthropic.com/) in the `ANTHROPIC_API_KEY` environment variable (only needed for classification).

```bash
docker compose up --build
```

To run the app from your IDE instead, start only the database with `docker compose up -d postgres` and run `./mvnw spring-boot:run` (Java 21).

The API runs on `http://localhost:8081`. Interactive docs: `http://localhost:8081/swagger-ui.html`.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/reviews/import?maxPages=30` | Imports up to `maxPages` × 100 reviews from Steam |
| `POST` | `/api/reviews/classify?limit=100` | Classifies up to `limit` pending reviews with Claude |
| `DELETE` | `/api/reviews/classifications` | Clears all categories (to re-run after a prompt change) |
| `GET` | `/api/reviews?category=BUG&votedUp=false&page=0&size=20` | Lists reviews, newest first |
| `GET` | `/api/reviews/stats` | Totals, % positive and counts per category |

Example:

```bash
curl -X POST "localhost:8081/api/reviews/import?maxPages=30"
# {"imported":2364,"skipped":0,"pages":24}

curl -X POST "localhost:8081/api/reviews/classify?limit=40"
# {"classified":39,"empty":1,"missing":0,"apiCalls":2}

curl "localhost:8081/api/reviews?category=BUG"
```

## Tests

```bash
./mvnw test
```

Tests mock the Claude classifier, so they need no API key and spend no credits. Integration tests (`*IT`) start a PostgreSQL container, so Docker must be running.
