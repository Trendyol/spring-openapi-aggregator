package com.trendyol.openapiaggregator.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.COMPONENTS;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.NOT_OBJECT;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.OBJECT_VALUE;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.TEST_FILTER;

class OpenApiFilterTransformerTest {
    private final OpenApiFilterTransformer transformer = new OpenApiFilterTransformer() {
        @Override public String getFilterName() { return TEST_FILTER; }
        @Override public void transform(ObjectNode openApiRoot, java.util.Map<String, String> filterArgs) { }
    };

    @Test
    void defaultMethodsReportEnabledAndCreateMissingObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        assertThat(transformer.isEnabled()).isTrue();
        ObjectNode created = transformer.ensureObjectNode(root, COMPONENTS, mapper);
        assertThat(created).isSameAs(root.path(COMPONENTS));
        assertThat(root.path(COMPONENTS).isObject()).isTrue();
    }

    @Test
    void defaultObjectNodeHelperReplacesNonObjectAndKeepsExistingObject() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put(OBJECT_VALUE, NOT_OBJECT);

        ObjectNode replaced = transformer.ensureObjectNode(root, OBJECT_VALUE, mapper);
        ObjectNode existing = transformer.ensureObjectNode(root, OBJECT_VALUE, mapper);

        assertThat(replaced).isSameAs(existing);
        assertThat(root.path(OBJECT_VALUE).isObject()).isTrue();
    }
}
