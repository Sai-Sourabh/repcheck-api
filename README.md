# RepCheck API

Backend for RepCheck, a lift tracker that tells you whether you are progressively overloading.

Spring Boot 3.3, Java 17, PostgreSQL, Flyway, JWT. Modular monolith. The web app lives in a separate repo, `repcheck-web`.

## Run locally

```bash
docker compose up -d          # PostgreSQL 16 on localhost:5432
./mvnw spring-boot:run        # API on http://localhost:8080
```

Flyway creates the 11 tables and seeds 60 exercises on first start.

- Swagger UI: http://localhost:8080/swagger-ui.html (register, copy the token, click Authorize)
- Health: http://localhost:8080/api/health

Needs JDK 17 or newer. On Windows use `mvnw.cmd`.

## Tests

```bash
./mvnw test                              # unit tests, no database needed
REPCHECK_IT=true ./mvnw verify           # plus the end-to-end API test (needs the docker DB running)
```

PowerShell: `$env:REPCHECK_IT="true"; .\mvnw.cmd verify`

GitHub Actions runs the full set against a Postgres service container on every push.

## Configuration

All settings have local defaults and are overridden by environment variables.

| Variable | Default | Notes |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/repcheck` | JDBC URL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `repcheck` | |
| `JWT_SECRET` | dev value | **Set this in production.** 32+ characters |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma separated. Add the Vercel URL here |
| `APP_TIMEZONE` | `Asia/Kolkata` | Defines "today" and week boundaries |
| `PORT` | `8080` | |

## How the numbers work

Everything is plain arithmetic in `domain/`, with no framework dependencies, so it is easy to test and tune.

| Thing | Rule |
| --- | --- |
| Estimated 1RM | Epley: `weight x (1 + reps / 30)` |
| Arrow (UP / DOWN / SAME) | Best estimated 1RM this session vs the previous session of the same exercise. Under 0.5% is SAME. Bodyweight lifts compare reps |
| PR | Best estimated 1RM beats every earlier session |
| Weekly sets per muscle | 1 per set as the primary muscle, 0.5 as a secondary muscle |
| ATL / CTL | 7 day and 28 day exponentially weighted averages of daily volume, seeded with the mean of the first 14 days |
| TSB | `CTL - ATL`. Decisions use TSB as a percentage of CTL, since raw volume differs a lot between lifters |
| Next week | `ProgressionRuleEngine`: compares weekly sets to MEV / MAV / MRV landmarks, the average lift change for that muscle, and fatigue. Returns INCREASE, MAINTAIN or DELOAD with a suggested set count |
| Streaks | Weekly. Workout streak = weeks hitting your sessions-per-week goal, steps streak = weeks hitting the steps goal. A missed week burns a freeze token; every 4th week in a row earns one (max 2) |

`exercise_history`, `daily_load` and `muscle_weekly_volume` are derived tables. They are rebuilt in the same transaction whenever a session is saved or deleted, so back-dating or deleting a workout never leaves a stale arrow. The scheduled jobs (nightly load roll-forward, Sunday volume, Monday streaks) only keep things fresh on days with no activity; reads top up on demand if a job was missed.

## API

| Area | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Profile | `GET /api/users/me`, `PATCH /api/users/me` |
| Exercises | `GET /api/exercises?muscle=&q=`, `POST /api/exercises`, `DELETE /api/exercises/{id}` |
| Templates | `GET, POST /api/templates`, `GET, PUT, DELETE /api/templates/{id}` |
| Workouts | `POST /api/workouts`, `GET /api/workouts?page=&size=`, `GET, DELETE /api/workouts/{id}`, `GET /api/workouts/previous?exerciseIds=` |
| Analytics | `GET /api/analytics/lifts`, `/exercises/{id}/history`, `/volume?date=`, `/fatigue?days=`, `/heatmap?days=`, `/balance`, `/prs` |
| Streaks | `GET /api/streaks` |
| Cardio | `POST /api/cardio`, `GET /api/cardio?days=`, `DELETE /api/cardio/{id}`, `GET /api/cardio/steps?date=` |
| Body weight | `POST /api/body-weight`, `GET /api/body-weight?days=` |

Responses are always DTOs (`dto/`), never JPA entities.

## Deploy

The included `Dockerfile` works on Render, Railway or Fly. Create a Postgres database (Neon or the host's own), then set `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET` and `CORS_ALLOWED_ORIGINS` (your Vercel URL). Then set `VITE_API_URL` in the web app to the API's public URL.

## Layout

```
controller/   REST endpoints
service/      transactions and orchestration
domain/       pure math: 1RM, fatigue, progression rules, volume landmarks
repository/   Spring Data JPA
entity/       JPA entities
dto/          request / response records
job/          scheduled jobs
security/     JWT
config/       security, CORS, OpenAPI, clock
exception/    error responses
```
