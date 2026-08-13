package com.trendyol.openapiaggregator.transformers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

/** Documents the query parameter removed by Spring Cloud Gateway's {@code RemoveRequestParameter} filter.
 *
 * @see <a href="https://docs.spring.io/spring-cloud-gateway/docs/4.1.x/reference/html/#the-removerequestparameter-gatewayfilter-factory">RemoveRequestParameter GatewayFilter Factory</a>
 */
public class RemoveRequestParameterOpenApiFilterTransformer implements OpenApiFilterTransformer {
    @Override public String getFilterName() { return "RemoveRequestParameter"; }

    @Override public void transform(ObjectNode root, Map<String, String> args) {
        String name = args.getOrDefault("name", args.get("_genkey_0"));
        if (name == null || name.isBlank()) return;
        PathTransformerSupport.forEachOperation(root, operation -> {
            if (!(operation.get("parameters") instanceof ArrayNode parameters)) return;
            Iterator<com.fasterxml.jackson.databind.JsonNode> iterator = parameters.iterator();
            while (iterator.hasNext()) {
                var parameter = iterator.next();
                if ("query".equals(parameter.path("in").asText()) && name.equals(parameter.path("name").asText())) iterator.remove();
            }
        });
    }
}
