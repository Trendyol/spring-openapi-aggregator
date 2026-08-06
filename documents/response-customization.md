# Response customization

Responses can be added to operations with a matching OpenAPI tag. An inline schema is placed in `components.schemas` and referenced by the response.

```yaml
openapi:
  aggregator:
    response-customization:
      tag-responses:
        auth:
          - status-code: "401"
            description: Authentication required
            schema-ref: '#/components/schemas/AuthenticationError'
            inline-schema:
              type: object
              properties:
                code:
                  type: string
                message:
                  type: string
              required:
                - code
                - message
```

If the upstream operation already has the status code, the configured response augments it instead of replacing the operation.
