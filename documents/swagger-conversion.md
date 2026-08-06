# Swagger 2.0 conversion

The aggregator accepts both Swagger 2.0 and OpenAPI documents. Swagger 2.0 documents are normalized to OpenAPI 3.0.1 before filtering and merging.

The conversion includes:

- `definitions` to `components.schemas`
- `securityDefinitions` to `components.securitySchemes`
- Swagger response schemas to `content.application/json.schema`
- Body parameters to `requestBody`
- `host`, `basePath`, and `schemes` to `servers`
- References from `#/definitions/...` to `#/components/schemas/...`

This allows services using different documentation versions to appear in one unified document.
