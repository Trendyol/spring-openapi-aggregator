package com.trendyol.openapiaggregator.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltInOpenApiFilterTransformersTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void prefixPathRemovesTheBackendPrefixFromPublicDocumentation() {
        ObjectNode root = document("/backend/orders/{id}");

        new PrefixPathOpenApiFilterTransformer().transform(root, Map.of("prefix", "/backend"));

        assertThat(root.path("paths").has("/orders/{id}")).isTrue();
        assertThat(root.path("paths").has("/backend/orders/{id}")).isFalse();
    }

    @Test
    void stripPrefixRemovesConfiguredPathParts() {
        ObjectNode root = document("/gateway/api/orders");

        new StripPrefixOpenApiFilterTransformer().transform(root, Map.of("parts", "2"));

        assertThat(root.path("paths").has("/orders")).isTrue();
    }

    @Test
    void rewritePathReconstructsThePublicPathFromNamedReplacementGroups() {
        ObjectNode root = document("/{id}");
        Map<String, String> args = Map.of(
                "regexp", "/public/(?<segment>.*)",
                "replacement", "/${segment}");

        new RewritePathOpenApiFilterTransformer().transform(root, args);

        assertThat(root.path("paths").has("/public/{id}")).isTrue();
    }

    @Test
    void setPathUsesTheConfiguredTemplateForTheDocumentedEndpoint() {
        ObjectNode root = document("/service/orders");

        new SetPathOpenApiFilterTransformer().transform(root, Map.of("template", "/public/orders"));

        assertThat(root.path("paths").has("/public/orders")).isTrue();
        assertThat(root.path("paths").has("/service/orders")).isFalse();
    }

    @Test
    void addRequestParameterDocumentsTheAddedQueryParameter() {
        ObjectNode root = document("/orders");

        new AddRequestParameterOpenApiFilterTransformer().transform(root,
                Map.of("name", "tenant", "value", "demo"));

        assertThat(root.at("/paths/~1orders/get/parameters/0/name").asText()).isEqualTo("tenant");
        assertThat(root.at("/paths/~1orders/get/parameters/0/schema/default").asText()).isEqualTo("demo");
    }

    @Test
    void removeRequestParameterRemovesOnlyTheMatchingQueryParameter() {
        ObjectNode root = document("/orders");
        ObjectNode operation = (ObjectNode) root.at("/paths/~1orders/get");
        var parameters = operation.putArray("parameters");
        parameters.addObject().put("name", "tenant").put("in", "query");
        parameters.addObject().put("name", "page").put("in", "query");

        new RemoveRequestParameterOpenApiFilterTransformer().transform(root, Map.of("name", "tenant"));

        assertThat(root.at("/paths/~1orders/get/parameters")).hasSize(1);
        assertThat(root.at("/paths/~1orders/get/parameters/0/name").asText()).isEqualTo("page");
    }

    @Test
    void transformersAcceptShortcutArguments() {
        ObjectNode root = document("/backend/items");

        new PrefixPathOpenApiFilterTransformer().transform(root, Map.of("prefix", "backend"));
        new SetPathOpenApiFilterTransformer().transform(root, Map.of("_genkey_0", "/items"));

        assertThat(root.path("paths").has("/items")).isTrue();
    }

    private ObjectNode document(String path) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode paths = root.putObject("paths");
        paths.putObject(path).putObject("get").putObject("responses").putObject("200");
        return root;
    }
}
