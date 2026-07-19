# Persons Finder — project conventions

## Workflow

- Never commit directly to `main`: create a branch, open a PR, squash to a
  single commit before merge.
- Branch names: `<type>/<short-description>`, e.g. `feat/nearby-search`.
- Every PR is a self-contained vertical slice: one piece of meaningful
  functionality, delivered together with its unit tests, API docs (OpenAPI
  annotations / README where relevant), and an e2e scenario. Avoid PRs that
  ship code without tests/docs, or tests/docs detached from the feature they
  belong to.

## PR titles & commit messages

Use Conventional Commits for PR titles and commit message subjects:

```
<type>: <imperative summary>
```

Types: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `ci`, `perf`.
Example: `feat: add nearby persons search`. Details go in the commit body as
bullet points.

## Build & test

- Local JDK is too new for Gradle 7.6.1 (needs JDK 11–19) — use the Docker
  variants in the Makefile.
- `make test` — unit tests (Docker, no local JDK). `make e2e` — Hurl e2e suite
  against the compose stack. `make` lists all targets.
- Requires a `.env` (`cp .env.example .env`); compose fails fast without it.
- CI (`.github/workflows/ci.yml`) runs unit + e2e on every push.

## Test layers

Test each layer at the layer, not through the one above it. Avoid duplicating
the same assertion across layers — each test should cover what only it can.

Test files mirror production classes: same package, one test class per
production class, named `<Class>Test` (e.g. `LocationRepositoryTest` for
`LocationRepository`). Don't create combined test files spanning several
classes.

- **Service** — pure unit tests with mocked repositories (Mockito). Cover logic
  and branches (e.g. not-found throwing), not persistence. See
  `*ServiceImplTest`.
- **Repository / persistence** — `@DataJpaTest` against Flyway-migrated H2
  (`@AutoConfigureTestDatabase(replace = NONE)` so the real V1 schema runs).
  Cover entity↔table mapping, id generation, nullability, and schema
  constraints (FKs, upsert-by-PK). See `PersonRepositoryTest`,
  `LocationRepositoryTest`.
- **Controller / web** — `@SpringBootTest` + MockMvc, `@Transactional` for
  rollback between tests. Cover status codes, response body, request validation
  and error-response shape — plus a light wiring check that a write reached the
  DB. Do **not** re-assert field-level persistence here (that's the repository
  test's job). See `PersonControllerTest`.
- **E2E** — curl-style Hurl scenarios in `e2e/tests/*.hurl`, one self-contained
  file per feature; `{{base_url}}` is provided by `e2e/run.sh`. See
  `e2e/README.md`.
