# AI_LOG.md

The commit history reflects how I worked with AI — read the commit titles top to
bottom (`git log --oneline`) to follow the process.

Below are the moments where I overrode the AI's first instinct:

1. **Secrets out of source.** AI hardcoded the DB credentials in
   `docker-compose.yml`; I moved them into `.env`.

2. **Haversine over PostGIS.** PostGIS means a much heavier image (~1.2 GB vs.
   ~250 MB). I kept it lean and did distance in plain SQL, deferring PostGIS to
   any future performance tuning.

3. **Table design and types.** AI wanted separate tables for hobbies and
   location. I kept a `location` table but folded hobbies into `person`, and
   changed `name`/`job_title`/`hobbies` from `text` to bounded `varchar`.

4. **No databases in unit tests.** AI missed service/repository unit tests, then
   reached for H2. I mocked collaborators instead and retired H2 — real
   persistence is verified once, in the e2e suite against PostgreSQL.

5. **Strict lat/lon typing.** AI accepted string coordinates; I enforced typed
   doubles with real bounds (±90 / ±180).

6. **A change-detector for the hand-written SQL.** AI thought a test for the
   `findNearby` native query was unnecessary. I added one that pins the query
   *text*, so any edit to the haversine SQL is a deliberate, reviewed change.

7. **A dedicated home for LLM calls.** I isolated everything that talks to the
   LLM into its own `llm/` package, so the AI boundary is easy to reason about
   and guard.
