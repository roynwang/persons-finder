# Persons Finder — project conventions

## Workflow

- Never commit directly to `main`: create a branch, open a PR, squash to a
  single commit before merge.
- Branch names: `<type>/<short-description>`, e.g. `feat/nearby-search`.

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

## E2E tests

Curl-style Hurl scenarios in `e2e/tests/*.hurl`, one self-contained file per
feature; `{{base_url}}` is provided by `e2e/run.sh`. See `e2e/README.md`.
