# E2E tests

Curl-style end-to-end tests using [Hurl](https://hurl.dev) (`brew install hurl`).
Each `.hurl` file under `tests/` is a scenario: plain HTTP requests with
assertions, run against the real docker compose stack (app + PostgreSQL).

## Run

```bash
e2e/run.sh                        # build + start stack, run everything
e2e/run.sh e2e/tests/health.hurl  # single scenario
SKIP_STACK=1 e2e/run.sh           # stack already running
BASE_URL=http://other-host:8080 SKIP_STACK=1 e2e/run.sh
```

An HTML report is written to `e2e/report/` (gitignored).

## Writing tests

- One file per scenario, named after the feature (`persons_crud.hurl`).
  Files run in alphabetical order; each file should be self-contained.
- `{{base_url}}` is provided by the runner.
- Chain requests within a file and pass values along with `[Captures]`:

```hurl
POST {{base_url}}/api/v1/persons
{ "name": "John" }

HTTP 200
[Captures]
person_id: jsonpath "$.id"

GET {{base_url}}/api/v1/persons/{{person_id}}

HTTP 200
[Asserts]
jsonpath "$.name" == "John"
```
