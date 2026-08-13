package com.trendyol.openapiaggregator.transformers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Transforms paths for Spring Cloud Gateway's {@code SetPath} filter.
 * <p>
 * {@code SetPath} replaces the request path with a configured URI template
 * before forwarding it. This transformer replaces the service document paths
 * with that configured template so the resulting OpenAPI document describes
 * the gateway endpoint.
 *
 * @see <a href="https://docs.spring.io/spring-cloud-gateway/docs/4.1.x/reference/html/#the-setpath-gatewayfilter-factory">SetPath GatewayFilter Factory</a>
 */
@RequiredArgsConstructor
public class SetPathOpenApiFilterTransformer implements OpenApiFilterTransformer {

    @Override public String getFilterName() { return "SetPath"; }

    @Override public void transform(ObjectNode root, Map<String, String> args) {
        String template = args.getOrDefault("template", args.get("_genkey_0"));
        if (template == null || template.isBlank()) {
            return;
        }
        // SetPath's template is the public path in the route definition. It is
        // also the only path information exposed by this filter's configuration.
        PathTransformerSupport.transform(root, path -> template);
    }
}
