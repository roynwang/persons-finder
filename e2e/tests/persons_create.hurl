# Create a person with all fields
POST {{base_url}}/api/v1/persons
{
  "name": "John Doe",
  "jobTitle": "Developer",
  "hobbies": "chess, hiking",
  "location": { "latitude": -36.8485, "longitude": 174.7633 }
}

HTTP 201
[Captures]
person_id: jsonpath "$.id"
[Asserts]
jsonpath "$.id" isInteger

# Optional fields can be omitted
POST {{base_url}}/api/v1/persons
{
  "name": "Minimal Person",
  "location": { "latitude": 0.0, "longitude": 0.0 }
}

HTTP 201

# Blank name is rejected with a field error
POST {{base_url}}/api/v1/persons
{
  "name": "",
  "location": { "latitude": 0.0, "longitude": 0.0 }
}

HTTP 400
[Asserts]
jsonpath "$.name" exists

# Out-of-range latitude is rejected
POST {{base_url}}/api/v1/persons
{
  "name": "Bad Latitude",
  "location": { "latitude": 91.0, "longitude": 0.0 }
}

HTTP 400

# A numeric field sent as a string is rejected (strict typing, not coerced)
POST {{base_url}}/api/v1/persons
{
  "name": "String Latitude",
  "location": { "latitude": "45.0", "longitude": 10.0 }
}

HTTP 400
[Asserts]
jsonpath "$['location.latitude']" exists
