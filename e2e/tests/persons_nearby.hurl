# Nearby search. The e2e database is shared and long-lived, so this scenario
# uses a patch of ocean unique to this file and asserts membership rather than
# exact counts (earlier runs leave duplicate rows at the same coordinates).

# Person at the query point
POST {{base_url}}/api/v1/persons
{
  "name": "Nearby Center",
  "jobTitle": "Explorer",
  "location": { "latitude": 10.0, "longitude": -42.0 }
}

HTTP 201
[Captures]
center_id: jsonpath "$.id"

# ~5.6 km north of the query point
POST {{base_url}}/api/v1/persons
{
  "name": "Nearby Close",
  "location": { "latitude": 10.05, "longitude": -42.0 }
}

HTTP 201
[Captures]
close_id: jsonpath "$.id"

# ~55.6 km north: outside the search radius
POST {{base_url}}/api/v1/persons
{
  "name": "Nearby Outside",
  "location": { "latitude": 10.5, "longitude": -42.0 }
}

HTTP 201
[Captures]
outside_id: jsonpath "$.id"

# The radius includes the first two and excludes the third; results are
# sorted closest first, so the head of the list is at ~0 km.
GET {{base_url}}/api/v1/persons/nearby?lat=10.0&lon=-42.0&radiusKm=25
HTTP 200
[Asserts]
jsonpath "$[*].id" contains {{center_id}}
jsonpath "$[*].id" contains {{close_id}}
jsonpath "$[*].id" not contains {{outside_id}}
jsonpath "$[0].distanceKm" < 0.1
# Details come through — assert on this run's own row (the shared DB holds
# same-named rows from earlier runs), selected by its captured id.
jsonpath "$[?(@.id=={{center_id}})].name" contains "Nearby Center"
jsonpath "$[?(@.id=={{center_id}})].jobTitle" contains "Explorer"

# Moving a person is reflected by the search (location upsert end-to-end):
# the far person moves to ~0.1 km from the query point...
PUT {{base_url}}/api/v1/persons/{{outside_id}}/location
{
  "latitude": 10.001,
  "longitude": -42.0
}

HTTP 204

# ...and now appears even in a 1 km search, which the close person does not.
GET {{base_url}}/api/v1/persons/nearby?lat=10.0&lon=-42.0&radiusKm=1
HTTP 200
[Asserts]
jsonpath "$[*].id" contains {{outside_id}}
jsonpath "$[*].id" not contains {{close_id}}

# Out-of-range latitude is rejected with a field error
GET {{base_url}}/api/v1/persons/nearby?lat=91&lon=0&radiusKm=5
HTTP 400
[Asserts]
jsonpath "$.lat" exists

# Non-numeric radius is rejected in the standard error shape
GET {{base_url}}/api/v1/persons/nearby?lat=0&lon=0&radiusKm=abc
HTTP 400
[Asserts]
jsonpath "$.radiusKm" exists

# Missing radius is rejected
GET {{base_url}}/api/v1/persons/nearby?lat=0&lon=0
HTTP 400
[Asserts]
jsonpath "$.radiusKm" exists

# Zero radius is rejected (must be positive)
GET {{base_url}}/api/v1/persons/nearby?lat=0&lon=0&radiusKm=0
HTTP 400
[Asserts]
jsonpath "$.radiusKm" exists

# An absurdly large radius is rejected (capped to avoid whole-table scans)
GET {{base_url}}/api/v1/persons/nearby?lat=0&lon=0&radiusKm=999999
HTTP 400
[Asserts]
jsonpath "$.radiusKm" exists
