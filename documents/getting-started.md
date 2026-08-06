# Getting started

Enable the aggregator in a Spring Cloud Gateway application:

```java
@SpringBootApplication
@EnableOpenApiAggregator
public class GatewayApplication {
}
```

Configure a route with a service URI:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: orders
          uri: https://orders.example.com
          predicates:
            - Path=/orders/**
```

The service document is available at `/v3/api-docs/orders`. The merged document is available at `/v3/api-docs/unified`.

If the service uses a document path other than `/v3/api-docs`, configure an override:

```yaml
openapi:
  aggregator:
    overrides:
      orders:
        base-url: /swagger.json
```
