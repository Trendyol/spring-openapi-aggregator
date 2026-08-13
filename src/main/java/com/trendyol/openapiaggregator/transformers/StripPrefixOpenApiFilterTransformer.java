package com.trendyol.openapiaggregator.transformers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Transforms paths for Spring Cloud Gateway's {@code StripPrefix} filter.
 * <p>
 * {@code StripPrefix} removes one or more leading path segments before a
 * request reaches the service. This transformer removes the configured number
 * of leading segments from the OpenAPI paths as well.
 *
 * @see <a href="https://docs.spring.io/spring-cloud-gateway/docs/4.1.x/reference/html/#the-stripprefix-gatewayfilter-factory">StripPrefix GatewayFilter Factory</a>
 */
@RequiredArgsConstructor
public class StripPrefixOpenApiFilterTransformer implements OpenApiFilterTransformer {

    @Override public String getFilterName() { return "StripPrefix"; }

    @Override public void transform(ObjectNode root, Map<String, String> args) {
        var value = args.getOrDefault("parts", args.get("_genkey_0"));
        if (value == null) {
            return;
        }
        int parts;
        try { parts = Integer.parseInt(value); } catch (NumberFormatException ignored) { return; }
        if (parts < 1) {
            return;
        }
        PathTransformerSupport.transform(root, path -> {
            String result = path;
            for (int i = 0; i < parts && result.length() > 1; i++) {
                int slash = result.indexOf('/', 1);
                result = slash < 0 ? "/" : result.substring(slash);
            }
            return result;
        });
    }
}
