package com.trendyol.openapiaggregator.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

import static com.trendyol.openapiaggregator.constants.OpenApiConstants.HTTP_METHODS;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.NAME;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.PARAMETERS;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.PARAMETER_IN;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.PARAMETER_IN_PATH;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.PATHS;

@UtilityClass
class PathTransformerSupport {

    static void forEachOperation(ObjectNode root, java.util.function.Consumer<ObjectNode> consumer) {
        JsonNode pathsNode = root.get(PATHS);
        if (!(pathsNode instanceof ObjectNode paths)) return;
        paths.properties().forEach(entry -> {
            if (!(entry.getValue() instanceof ObjectNode pathItem)) return;
            HTTP_METHODS.forEach(method -> {
                if (pathItem.get(method) instanceof ObjectNode operation) consumer.accept(operation);
            });
        });
    }

    static void transform(ObjectNode root, UnaryOperator<String> pathTransformer) {
        JsonNode pathsNode = root.get(PATHS);
        if (!(pathsNode instanceof ObjectNode paths)) {
            return;
        }

        ObjectNode transformed = JsonNodeFactory.instance.objectNode();
        paths.properties().forEach(entry -> {
            String target = pathTransformer.apply(entry.getKey());
            ObjectNode item = entry.getValue() instanceof ObjectNode object ? object : null;
            if (item != null) {
                Set<String> removed = pathParameters(entry.getKey());
                removed.removeAll(pathParameters(target));
                if (!removed.isEmpty()) {
                    item = removePathParameters(item, removed);
                }
            }
            transformed.set(target, item == null ? entry.getValue() : item);
        });
        root.set(PATHS, transformed);
    }

    private static ObjectNode removePathParameters(ObjectNode item, Set<String> names) {
        ObjectNode copy = item.deepCopy();
        HTTP_METHODS.forEach(method -> {
            if (!(copy.get(method) instanceof ObjectNode operation)
                    || !(operation.get(PARAMETERS) instanceof ArrayNode parameters)) {
                return;
            }
            ArrayNode kept = JsonNodeFactory.instance.arrayNode();
            parameters.forEach(parameter -> {
                if (!parameter.path(PARAMETER_IN).asText().equals(PARAMETER_IN_PATH)
                        || !names.contains(parameter.path(NAME).asText())) {
                    kept.add(parameter);
                }
            });
            operation.set(PARAMETERS, kept);
        });
        return copy;
    }

    private static Set<String> pathParameters(String path) {
        Set<String> result = new HashSet<>();
        int start = 0;
        while ((start = path.indexOf('{', start)) >= 0) {
            int end = path.indexOf('}', start);
            if (end < 0) {
                break;
            }
            result.add(path.substring(start + 1, end));
            start = end + 1;
        }
        return result;
    }
}
