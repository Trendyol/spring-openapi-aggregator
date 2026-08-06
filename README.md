# Spring OpenAPI Aggregator

Spring OpenAPI Aggregator is a library for exposing and combining API documentation from services behind a Spring Cloud Gateway. It fetches a document for each configured route, normalizes Swagger 2.0 documents to OpenAPI 3.0.1, applies route and tag filtering, adds configured responses, and merges the resulting documents.

## Usage

Add the library to a Spring Cloud Gateway application and apply `@EnableOpenApiAggregator` to the application class:

```java
@SpringBootApplication
@EnableOpenApiAggregator
public class GatewayApplication {
}
```

Define routes using Spring Cloud Gateway configuration. The route ID identifies the document source and is used by the aggregator when applying configuration.

The library exposes:

- `/v3/api-docs/{routeId}` for one route
- `/v3/api-docs/unified` for the merged document

Configuration is provided under `openapi.aggregator`. The library supports route predicates, tag and path filters, OpenAPI metadata, route-specific document paths, response customization, and custom filter transformers.

## How it works

1. The aggregator resolves the route and fetches its API document with `WebClient`.
2. Swagger 2.0 documents are converted to OpenAPI 3.0.1.
3. Route predicates and configured filters limit paths and operations exposed by the route.
4. Custom responses and application-provided transformers update the document.
5. The unified endpoint merges paths, schemas, security schemes, and tags from all successful routes.

See the [documents](documents/README.md) directory for focused configuration examples.
