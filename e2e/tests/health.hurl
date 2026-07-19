# App is up and talking to PostgreSQL
GET {{base_url}}/api/v1/health

HTTP 200
[Asserts]
jsonpath "$.status" == "UP"
jsonpath "$.database" startsWith "PostgreSQL"
