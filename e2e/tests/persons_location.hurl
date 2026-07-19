# Create a person to move around
POST {{base_url}}/api/v1/persons
{
  "name": "Location Mover",
  "location": { "latitude": 0.0, "longitude": 0.0 }
}

HTTP 201
[Captures]
person_id: jsonpath "$.id"

# Update their location
PUT {{base_url}}/api/v1/persons/{{person_id}}/location
{
  "latitude": 51.5074,
  "longitude": -0.1278
}

HTTP 204

# Updating again replaces, not duplicates (still 204)
PUT {{base_url}}/api/v1/persons/{{person_id}}/location
{
  "latitude": -36.8485,
  "longitude": 174.7633
}

HTTP 204

# Unknown person id is a 404 in the standard error shape
PUT {{base_url}}/api/v1/persons/999999/location
{
  "latitude": 0.0,
  "longitude": 0.0
}

HTTP 404
[Asserts]
jsonpath "$.error" exists

# Out-of-range longitude is rejected
PUT {{base_url}}/api/v1/persons/{{person_id}}/location
{
  "latitude": 0.0,
  "longitude": 181.0
}

HTTP 400
[Asserts]
jsonpath "$.longitude" exists

# Non-numeric person id is rejected in the standard error shape
PUT {{base_url}}/api/v1/persons/abc/location
{
  "latitude": 0.0,
  "longitude": 0.0
}

HTTP 400
[Asserts]
jsonpath "$.id" exists
