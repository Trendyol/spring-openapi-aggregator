package com.trendyol.openapiaggregator.transformers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/** Documents the query parameter added by Spring Cloud Gateway's {@code AddRequestParameter} filter.
 *
 * @see <a href="https://docs.spring.io/spring-cloud-gateway/docs/4.1.x/reference/html/#the-addrequestparameter-gatewayfilter-factory">AddRequestParameter GatewayFilter Factory</a>
 */
public class AddRequestParameterOpenApiFilterTransformer implements OpenApiFilterTransformer {
    @Override public String getFilterName() { return "AddRequestParameter"; }

    @Override public void transform(ObjectNode root, Map<String, String> args) {
        String name = args.getOrDefault("name", args.get("_genkey_0"));
        String value = args.getOrDefault("value", args.get("_genkey_1"));
        if (name == null || value == null || name.isBlank()) return;
        PathTransformerSupport.forEachOperation(root, operation -> {
            ArrayNode parameters = operation.withArray("parameters");
            for (var parameter : parameters) {
                if ("query".equals(parameter.path("in").asText()) && name.equals(parameter.path("name").asText())) return;
            }
            ObjectNode parameter = JsonNodeFactory.instance.objectNode();
            parameter.put("name", name).put("in", "query").put("required", false);
            parameter.putObject("schema").put("type", "string").put("default", value);
            parameters.add(parameter);
        });
    }
}
