# Filtering and route predicates

Route predicates are applied to the fetched document. Method predicates limit operations, while path predicates limit paths when the upstream document uses matching paths. If a gateway rewrite means that no upstream path matches, the document is retained rather than discarded.

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: orders-read
          uri: https://orders.example.com
          predicates:
            - Path=/orders/**
            - Method=GET
```

Additional tag and path filtering is configured per route:

```yaml
openapi:
  aggregator:
    filters:
      orders-read:
        include-tags:
          - public-orders
        exclude-paths:
          - /internal/health
```
