# Create a person, then fetch them by id.
# No assertion on "bio": it is null when generation is disabled (no API key)
# and races to non-null when a key is configured — the gated tests-llm suite
# owns that behavior.
POST {{base_url}}/api/v1/persons
{
  "name": "Get Me",
  "jobTitle": "Archivist",
  "hobbies": "origami",
  "location": { "latitude": 12.0, "longitude": 34.0 }
}

HTTP 201
[Captures]
person_id: jsonpath "$.id"

GET {{base_url}}/api/v1/persons/{{person_id}}

HTTP 200
[Asserts]
jsonpath "$.id" == {{person_id}}
jsonpath "$.name" == "Get Me"
jsonpath "$.jobTitle" == "Archivist"
jsonpath "$.hobbies" == "origami"

# Unknown person -> 404 with the standard error shape
GET {{base_url}}/api/v1/persons/999999999

HTTP 404
[Asserts]
jsonpath "$.error" exists

# Non-numeric id -> 400 with a field error
GET {{base_url}}/api/v1/persons/abc

HTTP 400
[Asserts]
jsonpath "$.id" exists
