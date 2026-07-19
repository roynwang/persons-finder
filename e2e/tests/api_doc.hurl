# OpenAPI spec is served and lists the API
GET {{base_url}}/v3/api-docs

HTTP 200
[Asserts]
jsonpath "$.openapi" startsWith "3."
jsonpath "$.paths['/api/v1/health']" exists
jsonpath "$.paths['/api/v1/health'].get.summary" == "Health check"
jsonpath "$.paths['/api/v1/health'].get.tags" includes "Health"

# Swagger UI is reachable
GET {{base_url}}/swagger-ui/index.html

HTTP 200
