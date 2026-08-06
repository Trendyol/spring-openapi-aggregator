package com.trendyol.openapiaggregator.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Interface for transforming OpenAPI specifications based on Spring Cloud Gateway filter configurations.
 */
public interface OpenApiFilterTransformer {

    /**
     * Returns the name of the Gateway filter this transformer handles.
     *
     * @return the filter name (e.g., "TransformPath", "RewritePath")
     */
    String getFilterName();

    /**
     * Transforms the OpenAPI specification based on the filter configuration.
     *
     * @param openApiRoot the root ObjectNode of the OpenAPI specification
     * @param filterArgs  the filter arguments from the route definition
     */
    void transform(ObjectNode openApiRoot, Map<String, String> filterArgs);

    /**
     * Indicates whether this transformer is enabled.
     *
     * @return true if enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Safely gets or creates an ObjectNode at the specified path.
     * This prevents ClassCastException when a node is missing or not an object.
     *
     * @param parent    the parent node
     * @param fieldName the field name
     * @param mapper    the ObjectMapper instance
     * @return the ObjectNode (existing or newly created)
     */
    default ObjectNode ensureObjectNode(ObjectNode parent, String fieldName, ObjectMapper mapper) {
        JsonNode node = parent.get(fieldName);
        if (node != null && node.isObject()) {
            return (ObjectNode) node;
        }
        ObjectNode newNode = mapper.createObjectNode();
        parent.set(fieldName, newNode);
        return newNode;
    }
}
