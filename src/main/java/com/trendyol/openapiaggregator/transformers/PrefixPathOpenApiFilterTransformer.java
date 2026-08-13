package com.trendyol.openapiaggregator.transformers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Transforms paths for Spring Cloud Gateway's {@code PrefixPath} filter.
 * <p>
 * {@code PrefixPath} adds a prefix before forwarding a request to the service.
 * The service OpenAPI document therefore contains the prefixed path; this
 * transformer removes that prefix so the documented path is the public gateway
 * path.
 *
 * @see <a href="https://docs.spring.io/spring-cloud-gateway/docs/4.1.x/reference/html/#the-prefixpath-gatewayfilter-factory">PrefixPath GatewayFilter Factory</a>
 */
@RequiredArgsConstructor
public class PrefixPathOpenApiFilterTransformer implements OpenApiFilterTransformer {

    private static String normalize(String value) {
        String result = value.trim();
        return result.startsWith("/") ? result : "/" + result;
    }

    @Override public String getFilterName() { return "PrefixPath"; }

    @Override public void transform(ObjectNode root, Map<String, String> args) {
        String prefix = args.get("prefix");
        if (prefix == null || prefix.isBlank()) {
            return;
        }
        prefix = normalize(prefix);
        String value = prefix;
        PathTransformerSupport.transform(root, path -> path.startsWith(value + "/")
                ? path.substring(value.length()) : path);
    }
}
