# Real-Gemini scenario — only runs via `make e2e-llm` (E2E_LLM=1), which
# requires GEMINI_API_KEY in .env. Bio generation is asynchronous, so poll the
# GET endpoint until the bio lands. Content is non-deterministic: assert
# presence and shape only, never wording.
POST {{base_url}}/api/v1/persons
{
  "name": "Bio E2E Person",
  "jobTitle": "Lighthouse Keeper",
  "hobbies": "kite surfing, sourdough baking",
  "location": { "latitude": -44.0, "longitude": -170.0 }
}

HTTP 201
[Captures]
person_id: jsonpath "$.id"

# Poll up to ~60s for the background worker to write the bio.
GET {{base_url}}/api/v1/persons/{{person_id}}
[Options]
retry: 30
retry-interval: 2000

HTTP 200
[Asserts]
jsonpath "$.bio" isString
jsonpath "$.bio" matches "\\S"
