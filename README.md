# 👥 Persons Finder

Backend for a mobile app that helps users find people around them.
Kotlin + Spring Boot + PostgreSQL, run via Docker Compose.

📄 See also: [AI_LOG.md](AI_LOG.md) — how I worked with AI · [SECURITY.md](SECURITY.md) — prompt injection & PII.

REST API (base path `/api/v1/persons`):

- `POST /persons` — create a person; generates a short, quirky AI bio from their
  job title and hobbies.
- `PUT /persons/{id}/location` — update a person's location.
- `GET /persons/nearby?lat=&lon=&radiusKm=` — find people around a point, sorted
  by distance (includes the AI bio).

**A couple of notes:**

- **CI** — GitHub Actions runs the unit and e2e suites on every push
  (`.github/workflows/ci.yml`).
- **Soft assert for the LLM call** — the bio is non-deterministic, so instead of
  matching exact text I added an LLM-as-judge test case. See
  [`src/eval/kotlin/com/persons/finder/llm/BioGeneratorEval.kt`](src/eval/kotlin/com/persons/finder/llm/BioGeneratorEval.kt)
  and [`src/eval/kotlin/com/persons/finder/llm/LlmJudge.kt`](src/eval/kotlin/com/persons/finder/llm/LlmJudge.kt).

---

## 🚀 How to run

Prerequisites: Docker running, and a `.env` file (`cp .env.example .env`).

```bash
make up      # build and start the app + PostgreSQL
make docs    # open Swagger UI in the browser
make down    # stop the stack
```

The app listens on <http://localhost:8080>. Run `make` to list all targets
(`logs`, `psql`, `clean`, ...).

> **Tip — Gemini API key.** Bio generation needs `GEMINI_API_KEY` in `.env` (get
> one from [Google AI Studio](https://aistudio.google.com/apikey)). It's
> optional: with no key the app runs fine and simply leaves bios null, so you
> only need it to see (or test) the AI bios.

---

## 🧪 Running tests

Prerequisites: Docker running, `brew install hurl`, and a `.env` file.

```bash
make test     # unit tests (in Docker — no local JDK needed)
make e2e      # build + start stack, run the Hurl e2e suite
make eval     # LLM eval suite against real Gemini (needs GEMINI_API_KEY in .env)
make e2e-llm  # e2e incl. the real-Gemini bio scenario (needs GEMINI_API_KEY in .env)
```

Unit tests and the plain e2e suite run in CI on every push; the LLM suites are
local-only and are skipped automatically when `GEMINI_API_KEY` is not set.
See [e2e/README.md](e2e/README.md) for how to write new e2e scenarios.

---

## 📖 API documentation

Interactive docs are generated automatically from the controllers by
[springdoc-openapi](https://springdoc.org/). With the stack running (`make up`):

- Swagger UI: <http://localhost:8080/swagger-ui/index.html> (`make docs` opens it)
- OpenAPI spec (JSON): <http://localhost:8080/v3/api-docs>

---

## 🤖 The LLM part

The AI bio lives in its own module under
[`src/main/kotlin/com/persons/finder/llm`](src/main/kotlin/com/persons/finder/llm).
Three things worth knowing:

- **Async task.** The bio is generated on a background worker, so creating a
  person doesn't wait on the model; if the call fails the bio is left null.
- **Self-contained.** All LLM code sits behind the `BioGenerator` interface in one
  package, so the provider can be swapped without touching callers.
- **Eval tests.** The output is non-deterministic, so it's checked with an
  LLM-as-judge eval rather than exact matching (`make eval`) — see
  [`BioGeneratorEval.kt`](src/eval/kotlin/com/persons/finder/llm/BioGeneratorEval.kt).

## 🚧 Unfinished / next steps

1. **Benchmark is not included.** For the 1M-row scalability goal I'd migrate the
   nearby search to PostGIS (a spatial index) rather than scan-and-compute.
2. **Nearby results are unbounded.** They should be capped/paginated — populating
   an unlimited result set could explode memory and DB load.
3. **Haversine runs in the DB.** Computing distance per row adds extra work on the
   database; a spatial index (PostGIS) would move most of that off the hot path.
4. **The LLM integration is minimal.** It should be designed more carefully —
   support multiple models, use a proper SDK instead of plain HTTP requests, and
   add retry, timeout, and backoff.
