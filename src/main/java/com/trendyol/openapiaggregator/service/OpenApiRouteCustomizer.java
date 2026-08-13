package com.trendyol.openapiaggregator.service;

import com.trendyol.openapiaggregator.configuration.OpenApiFilterConfig;
import com.trendyol.openapiaggregator.configuration.OpenApiInfoConfig;
import com.trendyol.openapiaggregator.configuration.ResponseCustomizationConfig;
import com.trendyol.openapiaggregator.dto.ServiceApiDoc;
import com.trendyol.openapiaggregator.exception.OpenApiAggregatorException;
import com.trendyol.openapiaggregator.exception.OpenApiTransformException;
import com.trendyol.openapiaggregator.transformers.OpenApiFilterTransformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.trendyol.openapiaggregator.constants.OpenApiConstants.APPLICATION_JSON;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.COMPONENTS;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.CONTENT;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.DESCRIPTION;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.ENUM;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.FORMAT;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.HTTP_METHODS;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.INFO;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.NAME;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.OPENAPI;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.OPENAPI_VERSION_3_0_1;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.OPERATION_ID;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.PATHS;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.PROPERTIES;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.REQUIRED;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.RESPONSES;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.SCHEMA;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.SCHEMAS;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.SCHEMA_REF;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.SECURITY_SCHEMES;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.TAGS;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.TITLE;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.TYPE;
import static com.trendyol.openapiaggregator.constants.OpenApiConstants.VERSION;

@Slf4j
@RequiredArgsConstructor
public class OpenApiRouteCustomizer {

    private final ObjectMapper mapper;
    private final ResponseCustomizationConfig config;
    private final OpenApiFilterConfig openApiFilterConfig;
    private final OpenApiInfoConfig openApiInfoConfig;
    private final List<OpenApiFilterTransformer> filterTransformers;


    public String applyOpenApiCustomisations(ServiceApiDoc serviceApiDoc,
                                             Map<String, Map<String, String>> predicates,
                                             Map<String, Object> filterConfigs) {
        try {
            ObjectNode mutableRoot = normalizeToOpenApi3(readObjectNode(serviceApiDoc.getApiDocs()));
            filterOpenApiSpecByPathAndTag(mutableRoot, serviceApiDoc.getServiceName());
            updateRoutesForFilters(mutableRoot, filterConfigs);
            applyCustomResponsesByTag(mutableRoot);
            filterPathsByRoutePredicates(mutableRoot, predicates);
            return mapper.writeValueAsString(mutableRoot);
        } catch (Exception exception) {
            throw new OpenApiTransformException("Failed to apply OpenAPI customizations", exception);
        }
    }

    public String mergeApiDocs(List<ServiceApiDoc> serviceDocs) {
        try {
            if (serviceDocs.isEmpty()) {
                return createEmptyOpenApiSpec();
            }

            ObjectNode mergedSpec = normalizeToOpenApi3(readObjectNode(serviceDocs.getFirst().getApiDocs()));

            updateInfoSection(mergedSpec);

            JsonNode pathsNode = mergedSpec.path(PATHS);
            ObjectNode mergedPaths;
            if (pathsNode.isMissingNode() || !pathsNode.isObject()) {
                mergedPaths = mapper.createObjectNode();
                mergedSpec.set(PATHS, mergedPaths);
            } else {
                mergedPaths = (ObjectNode) pathsNode;
            }

            prefixOperationIdsInPaths(mergedPaths, serviceDocs.getFirst().getServiceName());

            ObjectNode mergedComponents = ensureNode(mergedSpec, COMPONENTS);
            ObjectNode mergedSchemas = ensureNode(mergedComponents, SCHEMAS);
            ObjectNode mergedSecuritySchemes = ensureNode(mergedComponents, SECURITY_SCHEMES);

            JsonNode tagsNode = mergedSpec.path(TAGS);
            ArrayNode mergedTags;
            if (tagsNode.isMissingNode() || !tagsNode.isArray()) {
                mergedTags = mapper.createArrayNode();
                mergedSpec.set(TAGS, mergedTags);
            } else {
                mergedTags = (ArrayNode) tagsNode;
            }

            Set<String> addedTags = new HashSet<>();

            mergedTags.forEach(tag -> {
                if (tag.has(NAME)) {
                    addedTags.add(tag.get(NAME).asText());
                }
            });

            for (int i = 1; i < serviceDocs.size(); i++) {
                ServiceApiDoc serviceDoc = serviceDocs.get(i);
                ObjectNode serviceSpec = normalizeToOpenApi3(readObjectNode(serviceDoc.getApiDocs()));
                String serviceName = serviceDoc.getServiceName();

                mergePaths(mergedPaths, serviceSpec, serviceName);
                mergeSchemas(mergedSchemas, serviceSpec);
                mergeSecuritySchemes(mergedSecuritySchemes, serviceSpec);
                mergeTags(mergedTags, serviceSpec, addedTags);
            }

            removeEmptyPaths(mergedPaths);

            return mapper.writeValueAsString(mergedSpec);
        } catch (OpenApiAggregatorException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OpenApiAggregatorException("Failed to merge API docs", exception);
        }
    }

    private void updateInfoSection(ObjectNode spec) {
        ObjectNode info = ensureNode(spec, INFO);
        info.put(TITLE, openApiInfoConfig.getTitle());
        info.put(DESCRIPTION, openApiInfoConfig.getDescription());
        info.put(VERSION, openApiInfoConfig.getVersion());
    }

    private ObjectNode normalizeToOpenApi3(ObjectNode document) {
        if (!"2.0".equals(document.path("swagger").asText())) {
            return document;
        }

        document.remove("swagger");
        document.put(OPENAPI, OPENAPI_VERSION_3_0_1);

        ObjectNode components = ensureNode(document, COMPONENTS);
        JsonNode definitions = document.remove("definitions");
        if (definitions != null && definitions.isObject()) {
            components.set(SCHEMAS, rewriteRefs(definitions));
        }

        JsonNode securityDefinitions = document.remove("securityDefinitions");
        if (securityDefinitions != null && securityDefinitions.isObject()) {
            components.set("securitySchemes", rewriteRefs(securityDefinitions));
        }

        String host = document.path("host").asText("");
        String basePath = document.path("basePath").asText("/");
        if (!host.isEmpty()) {
            ObjectNode server = mapper.createObjectNode();
            server.put("url", (document.path("schemes").isArray() && !document.path("schemes").isEmpty()
                    ? document.path("schemes").get(0).asText() : "http") + "://" + host + basePath);
            document.set("servers", mapper.createArrayNode().add(server));
        }
        document.remove("host");
        document.remove("basePath");
        document.remove("schemes");

        JsonNode paths = document.get(PATHS);
        if (paths != null && paths.isObject()) {
            paths.properties().forEach(path -> normalizePathItem((ObjectNode) path.getValue()));
        }
        return (ObjectNode) rewriteRefs(document);
    }

    private ObjectNode readObjectNode(String apiDocs) throws Exception {
        JsonNode document = mapper.readTree(apiDocs);
        if (document instanceof ObjectNode objectNode) {
            return objectNode;
        }
        throw new OpenApiAggregatorException("API documentation must contain a JSON object");
    }

    private void normalizePathItem(ObjectNode pathItem) {
        for (String method : HTTP_METHODS) {
            JsonNode operationNode = pathItem.get(method);
            if (!(operationNode instanceof ObjectNode operation)) {
                continue;
            }

            ArrayNode parameters = operation.has("parameters") && operation.get("parameters").isArray()
                    ? (ArrayNode) operation.get("parameters") : null;
            if (parameters != null) {
                ArrayNode retained = mapper.createArrayNode();
                for (JsonNode parameter : parameters) {
                    if ("body".equals(parameter.path("in").asText())) {
                        ObjectNode requestBody = mapper.createObjectNode();
                        ObjectNode content = mapper.createObjectNode();
                        ObjectNode mediaType = mapper.createObjectNode();
                        mediaType.set(SCHEMA, parameter.path(SCHEMA).isMissingNode()
                                ? mapper.createObjectNode() : parameter.get(SCHEMA));
                        content.set(APPLICATION_JSON, mediaType);
                        requestBody.set("content", content);
                        if (parameter.path("required").asBoolean(false)) {
                            requestBody.put("required", true);
                        }
                        operation.set("requestBody", requestBody);
                    } else {
                        ObjectNode converted = parameter.deepCopy();
                        converted.remove("type");
                        converted.remove("format");
                        converted.remove("items");
                        ObjectNode schema = mapper.createObjectNode();
                        String type = parameter.path("type").asText(null);
                        if (type != null) {
                            schema.put("type", type);
                        }
                        if (parameter.has("format")) {
                            schema.put("format", parameter.get("format").asText());
                        }
                        if (parameter.has("items")) {
                            schema.set("items", parameter.get("items"));
                        }
                        converted.set(SCHEMA, schema);
                        retained.add(converted);
                    }
                }
                operation.set("parameters", retained);
            }

            JsonNode responses = operation.get(RESPONSES);
            if (responses != null && responses.isObject()) {
                responses.properties().forEach(entry -> {
                    if (entry.getValue() instanceof ObjectNode response && response.has(SCHEMA)) {
                        ObjectNode schema = (ObjectNode) response.remove(SCHEMA);
                        ObjectNode mediaType = mapper.createObjectNode();
                        mediaType.set(SCHEMA, schema);
                        ObjectNode content = mapper.createObjectNode();
                        content.set(APPLICATION_JSON, mediaType);
                        response.set(CONTENT, content);
                    }
                });
            }
        }
    }

    private JsonNode rewriteRefs(JsonNode node) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            object.properties().forEach(entry -> {
                if (SCHEMA_REF.equals(entry.getKey()) && entry.getValue().isTextual()) {
                    object.put(SCHEMA_REF, entry.getValue().asText().replace("#/definitions/", "#/components/schemas/"));
                } else {
                    rewriteRefs(entry.getValue());
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::rewriteRefs);
        }
        return node;
    }

    private void mergePaths(ObjectNode mergedPaths, ObjectNode serviceSpec, String serviceName) {
        JsonNode servicePathsNode = serviceSpec.path(PATHS);
        if (servicePathsNode.isObject()) {
            ObjectNode servicePaths = (ObjectNode) servicePathsNode;
            for (Map.Entry<String, JsonNode> entry : servicePaths.properties()) {
                String path = entry.getKey();
                if (!(entry.getValue() instanceof ObjectNode pathItem)) {
                    throw new OpenApiAggregatorException("Path is not an object while merging service "
                            + serviceName + ": " + path);
                }

                prefixOperationIds(pathItem, serviceName);

                if (mergedPaths.has(path)) {
                    if (!(mergedPaths.get(path) instanceof ObjectNode existingPathItem)) {
                        throw new OpenApiAggregatorException("Path is not an object while merging: " + path);
                    }
                    mergePathOperations(existingPathItem, pathItem, serviceName, path);
                } else {
                    mergedPaths.set(path, pathItem);
                }
            }
        }
    }

    private void prefixOperationIdsInPaths(ObjectNode paths, String serviceName) {
        paths.properties().forEach(entry -> {
            if (entry.getValue() instanceof ObjectNode pathItem) {
                prefixOperationIds(pathItem, serviceName);
            }
        });
    }

    private void prefixOperationIds(ObjectNode pathItem, String serviceName) {
        for (String method : HTTP_METHODS) {
            if (pathItem.has(method)) {
                ObjectNode operation = (ObjectNode) pathItem.get(method);
                if (operation.has(OPERATION_ID)) {
                    String operationId = operation.get(OPERATION_ID).asText();
                    operation.put(OPERATION_ID, serviceName + "_" + operationId);
                }
            }
        }
    }

    private void mergePathOperations(ObjectNode existingPathItem, ObjectNode newPathItem,
                                     String serviceName, String path) {
        for (String method : HTTP_METHODS) {
            if (newPathItem.has(method)) {
                if (existingPathItem.has(method)) {
                    throw new OpenApiAggregatorException("Conflicting " + method.toUpperCase()
                            + " operation while merging service " + serviceName + " at path " + path);
                }
                ObjectNode operation = (ObjectNode) newPathItem.get(method);
                existingPathItem.set(method, operation);
            }
        }
    }

    private void mergeSchemas(ObjectNode mergedSchemas, ObjectNode serviceSpec) {
        JsonNode componentsNode = serviceSpec.path(COMPONENTS);
        if (componentsNode.isObject()) {
            ObjectNode components = (ObjectNode) componentsNode;
            JsonNode schemasNode = components.path(SCHEMAS);
            if (schemasNode.isObject()) {
                ObjectNode schemas = (ObjectNode) schemasNode;
                schemas.properties().forEach(entry -> {
                    JsonNode existing = mergedSchemas.get(entry.getKey());
                    if (existing != null && !existing.equals(entry.getValue())) {
                        throw new OpenApiAggregatorException("Conflicting schema while merging: " + entry.getKey());
                    }
                    mergedSchemas.set(entry.getKey(), entry.getValue());
                });
            }
        }
    }

    private void mergeSecuritySchemes(ObjectNode mergedSecuritySchemes, ObjectNode serviceSpec) {
        JsonNode componentsNode = serviceSpec.path(COMPONENTS);
        if (componentsNode.isObject()) {
            ObjectNode components = (ObjectNode) componentsNode;
            JsonNode schemesNode = components.path(SECURITY_SCHEMES);
            if (schemesNode.isObject()) {
                mergeScheme(mergedSecuritySchemes, schemesNode);
            }
        }
    }

    private void mergeScheme(ObjectNode mergedScheme, JsonNode targetNode) {
        targetNode.properties().forEach(entry -> {
            String schemeName = entry.getKey();
            JsonNode schemeNode = entry.getValue();
            if (mergedScheme.has(schemeName)) {
                if (!mergedScheme.get(schemeName).equals(schemeNode)) {
                    throw new OpenApiAggregatorException(
                            "Conflicting security scheme while merging: " + schemeName);
                }
            } else {
                mergedScheme.set(schemeName, schemeNode);
            }
        });
    }

    private void mergeTags(ArrayNode mergedTags, ObjectNode serviceSpec, Set<String> addedTags) {
        JsonNode serviceTagsNode = serviceSpec.path(TAGS);
        if (serviceTagsNode.isArray()) {
            serviceTagsNode.forEach(tag -> {
                if (tag.has(NAME)) {
                    String tagName = tag.get(NAME).asText();
                    if (addedTags.add(tagName)) {
                        mergedTags.add(tag);
                    }
                }
            });
        }
    }

    private String createEmptyOpenApiSpec() {
        ObjectNode root = mapper.createObjectNode();
        root.put(OPENAPI, OPENAPI_VERSION_3_0_1);

        ObjectNode info = mapper.createObjectNode();
        info.put(TITLE, openApiInfoConfig.getTitle());
        info.put(DESCRIPTION, openApiInfoConfig.getNoServicesDescription());
        info.put(VERSION, openApiInfoConfig.getVersion());
        root.set(INFO, info);

        root.set(PATHS, mapper.createObjectNode());
        ObjectNode components = mapper.createObjectNode();
        components.set(SCHEMAS, mapper.createObjectNode());
        components.set(SECURITY_SCHEMES, mapper.createObjectNode());
        root.set(COMPONENTS, components);
        root.set(TAGS, mapper.createArrayNode());

        return root.toString();
    }

    private void filterPathsByRoutePredicates(ObjectNode mutableRoot,
                                               Map<String, Map<String, String>> predicates) {
        Map<String, String> pathPredicate = predicates.getOrDefault("Path", Map.of());
        List<String> pathPatterns = new ArrayList<>(pathPredicate.values());
        if (!pathPatterns.isEmpty()) {
            filterPathsByPatterns(mutableRoot, pathPatterns);
        }

        Map<String, String> methodPredicate = predicates.getOrDefault("Method", Map.of());
        if (!methodPredicate.isEmpty()) {
            Set<String> allowedMethods = methodPredicate.values().stream()
                    .flatMap(value -> Arrays.stream(value.split(",")))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            filterOperationsByMethods(mutableRoot, allowedMethods);
        }
    }

    private void filterPathsByPatterns(ObjectNode root, List<String> pathPatterns) {
        ObjectNode paths = ensureNode(root, PATHS);
        Iterator<Map.Entry<String, JsonNode>> fields = paths.properties().iterator();
        List<String> pathsToRemove = new ArrayList<>();
        Set<String> matchingPaths = new HashSet<>();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String path = entry.getKey();
            boolean matches = pathPatterns.stream()
                    .map(this::convertGatewayPatternToRegex)
                    .anyMatch(path::matches);
            if (matches) {
                matchingPaths.add(path);
            } else {
                pathsToRemove.add(path);
            }
        }

        // Gateway paths can be rewritten before reaching the upstream service.
        // If none of the upstream paths match, leave the document untouched.
        if (!matchingPaths.isEmpty()) {
            pathsToRemove.forEach(paths::remove);
        }
    }

    private void filterOperationsByMethods(ObjectNode root, Set<String> allowedMethods) {
        ObjectNode paths = ensureNode(root, PATHS);
        paths.properties().forEach(entry -> {
            if (entry.getValue() instanceof ObjectNode pathItem) {
                HTTP_METHODS.stream()
                        .filter(method -> !allowedMethods.contains(method))
                        .forEach(pathItem::remove);
            }
        });
        removeEmptyPaths(paths);
    }

    private void removeEmptyPaths(ObjectNode paths) {
        List<String> emptyPaths = new ArrayList<>();
        paths.properties().forEach(entry -> {
            if (entry.getValue() instanceof ObjectNode pathItem
                    && HTTP_METHODS.stream().noneMatch(pathItem::has)) {
                emptyPaths.add(entry.getKey());
            }
        });
        emptyPaths.forEach(paths::remove);
    }

    private String convertGatewayPatternToRegex(String gatewayPattern) {
        String pattern = gatewayPattern;
        if (!pattern.startsWith("^")) {
            pattern = "^" + pattern;
        }

        // Convert path variables {var} to regex BEFORE escaping
        pattern = pattern.replaceAll("\\{[^}]+}", "<<PATH_VAR>>");

        // Convert wildcards to placeholders BEFORE escaping
        pattern = pattern.replace("**", "<<DOUBLE_WILDCARD>>");
        pattern = pattern.replace("*", "<<SINGLE_WILDCARD>>");
        pattern = pattern.replace("?", "<<QUESTION>>");

        // Escape special regex characters
        pattern = pattern.replaceAll("([.+\\[\\](){}|\\\\])", "\\\\$1");

        // Replace placeholders with regex
        pattern = pattern.replace("<<PATH_VAR>>", "[^/]+");
        pattern = pattern.replace("<<DOUBLE_WILDCARD>>", ".*");
        pattern = pattern.replace("<<SINGLE_WILDCARD>>", "[^/]*");
        pattern = pattern.replace("<<QUESTION>>", ".");

        return pattern;
    }

    private void updateRoutesForFilters(ObjectNode mutableRoot, Map<String, Object> filterConfigs) {
        filterTransformers.stream()
                .filter(transformer -> transformer.isEnabled()
                        && !openApiFilterConfig.getDisabledTransformers().contains(transformer.getFilterName()))
                .forEach(transformer -> {
                    Object configuredArgs = filterConfigs.get(transformer.getFilterName());
                    if (configuredArgs instanceof Map<?, ?> rawArgs) {
                        Map<String, String> args = rawArgs.entrySet().stream()
                                .filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof String)
                                .collect(Collectors.toMap(
                                        entry -> (String) entry.getKey(),
                                        entry -> (String) entry.getValue()));
                        if (!args.isEmpty()) {
                            transformer.transform(mutableRoot, args);
                        }
                    }
                });
    }

    private void applyCustomResponsesByTag(ObjectNode mutableRoot) {
        config.getTagResponses().forEach((tag, responses) ->
                addResponsesToTaggedOperations(mutableRoot, tag, responses));
    }

    private void filterOpenApiSpecByPathAndTag(ObjectNode mutableRoot, String serviceName) {
        filterPathsByTags(mutableRoot, serviceName);
        filterTagsArray(mutableRoot, serviceName);
    }

    private void filterPathsByTags(ObjectNode root, String serviceName) {
        OpenApiFilterConfig.ServiceFilter filter = openApiFilterConfig.getFilters().getOrDefault(serviceName, new OpenApiFilterConfig.ServiceFilter());
        List<String> includeTags = filter.getIncludeTags();
        List<String> excludeTags = filter.getExcludeTags();
        List<String> excludePaths = filter.getExcludePaths();

        ObjectNode paths = ensureNode(root, PATHS);
        Iterator<Map.Entry<String, JsonNode>> fields = paths.properties().iterator();
        List<String> pathsToRemove = new ArrayList<>();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = fields.next();
            String path = pathEntry.getKey();
            ObjectNode pathItem = (ObjectNode) pathEntry.getValue();

            // Check if path is in exclude list
            if (excludePaths.contains(path)) {
                pathsToRemove.add(path);
                continue;
            }

            // Check tag filtering
            boolean shouldKeepPath = shouldKeepPathByTags(pathItem, includeTags, excludeTags);
            if (!shouldKeepPath) {
                pathsToRemove.add(path);
            }
        }

        pathsToRemove.forEach(paths::remove);
    }

    private boolean shouldKeepPathByTags(ObjectNode pathItem, List<String> includeTags, List<String> excludeTags) {
        boolean hasMatchingIncludeTag = includeTags.isEmpty();
        boolean hasMatchingExcludeTag = false;

        for (String method : HTTP_METHODS) {
            if (pathItem.has(method)) {
                ObjectNode operation = (ObjectNode) pathItem.get(method);

                // Check exclude tags
                if (!excludeTags.isEmpty()) {
                    for (String excludeTag : excludeTags) {
                        if (hasTag(operation, excludeTag)) {
                            hasMatchingExcludeTag = true;
                            break;
                        }
                    }
                }

                // Check include tags
                if (!includeTags.isEmpty()) {
                    for (String includeTag : includeTags) {
                        if (hasTag(operation, includeTag)) {
                            hasMatchingIncludeTag = true;
                            break;
                        }
                    }
                }
            }
        }

        return hasMatchingIncludeTag && !hasMatchingExcludeTag;
    }

    private void filterTagsArray(ObjectNode root, String serviceName) {
        OpenApiFilterConfig.ServiceFilter filter = openApiFilterConfig.getFilters().getOrDefault(serviceName, new OpenApiFilterConfig.ServiceFilter());
        List<String> includeTags = filter.getIncludeTags();
        List<String> excludeTags = filter.getExcludeTags();

        if (includeTags.isEmpty() && excludeTags.isEmpty()) {
            return;
        }

        JsonNode tagsNode = root.get(TAGS);
        if (tagsNode == null || !tagsNode.isArray()) {
            return;
        }

        ArrayNode tagsArray = (ArrayNode) tagsNode;
        ArrayNode filteredTags = mapper.createArrayNode();

        for (JsonNode tagNode : tagsArray) {
            if (tagNode.has(NAME)) {
                String tagName = tagNode.get(NAME).asText();
                boolean shouldInclude = true;

                if (!includeTags.isEmpty()) {
                    shouldInclude = includeTags.contains(tagName);
                }

                if (shouldInclude && !excludeTags.isEmpty()) {
                    shouldInclude = !excludeTags.contains(tagName);
                }

                if (shouldInclude) {
                    filteredTags.add(tagNode);
                }
            }
        }

        root.set(TAGS, filteredTags);
    }


    private void addResponsesToTaggedOperations(ObjectNode root, String tag, List<ResponseCustomizationConfig.CustomResponse> responses) {
        ObjectNode paths = ensureNode(root, PATHS);

        paths.properties().forEach(pathEntry -> {
            ObjectNode pathItem = (ObjectNode) pathEntry.getValue();

            for (String method : HTTP_METHODS) {
                if (pathItem.has(method)) {
                    ObjectNode operation = (ObjectNode) pathItem.get(method);
                    if (hasTag(operation, tag)) {
                        addCustomResponsesToOperation(root, operation, responses);
                    }
                }
            }
        });
    }

    private void addCustomResponsesToOperation(ObjectNode root, ObjectNode operation, List<ResponseCustomizationConfig.CustomResponse> customResponses) {
        ObjectNode responsesNode = ensureNode(operation, RESPONSES);

        for (ResponseCustomizationConfig.CustomResponse response : customResponses) {
            // Skip if response already exists
            String statusCode = Optional.ofNullable(response.getStatusCode()).orElse("200");

            // Add schema if inline schema is defined
            var inlineSchema = response.getInlineSchema();
            if (inlineSchema != null) {
                extractSchemaNameFromRef(response.getSchemaRef())
                        .ifPresent(schemaName -> addSchemaDefinition(root, schemaName, inlineSchema));
            }

            ObjectNode responseNode;
            JsonNode existingResponse = responsesNode.get(statusCode);
            if (existingResponse != null && existingResponse.isObject()) {
                responseNode = (ObjectNode) existingResponse;
            } else {
                responseNode = mapper.createObjectNode();
                responsesNode.set(statusCode, responseNode);
            }

            if (response.getDescription() != null) {
                responseNode.put(DESCRIPTION, response.getDescription());
            } else if (!responseNode.has(DESCRIPTION)) {
                responseNode.put(DESCRIPTION, "");
            }

            if (response.getSchemaRef() != null) {
                ObjectNode schemaNode = mapper.createObjectNode();
                schemaNode.put(SCHEMA_REF, response.getSchemaRef());
                ObjectNode contentNode = mapper.createObjectNode();
                ObjectNode appJsonNode = mapper.createObjectNode();
                appJsonNode.set(SCHEMA, schemaNode);
                contentNode.set(APPLICATION_JSON, appJsonNode);
                responseNode.set(CONTENT, contentNode);
            }
        }
    }

    private Optional<String> extractSchemaNameFromRef(@Nullable String schemaRef) {
        if (schemaRef == null) {
            return Optional.empty();
        }
        int lastSlash = schemaRef.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < schemaRef.length() - 1) {
            return Optional.of(schemaRef.substring(lastSlash + 1));
        }
        return Optional.empty();
    }

    private void addSchemaDefinition(ObjectNode root, String schemaName, ResponseCustomizationConfig.SchemaDefinition schemaDef) {
        ObjectNode components = ensureNode(root, COMPONENTS);
        ObjectNode schemas = ensureNode(components, SCHEMAS);

        // Don't overwrite existing schemas
        if (schemas.has(schemaName)) {
            return;
        }

        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put(TYPE, Optional.ofNullable(schemaDef.getType()).orElse("object"));

        if (schemaDef.getDescription() != null) {
            schemaNode.put(DESCRIPTION, schemaDef.getDescription());
        }

        if (schemaDef.getProperties() != null) {
            ObjectNode propertiesNode = mapper.createObjectNode();
            schemaDef.getProperties().forEach((propName, propDef) -> {
                ObjectNode propertyNode = mapper.createObjectNode();
                propertyNode.put(TYPE, Optional.ofNullable(propDef.getType()).orElse("string"));
                if (propDef.getFormat() != null) {
                    propertyNode.put(FORMAT, propDef.getFormat());
                }
                if (propDef.getDescription() != null) {
                    propertyNode.put(DESCRIPTION, propDef.getDescription());
                }
                if (propDef.getEnumValues() != null) {
                    ArrayNode enumArray = mapper.createArrayNode();
                    propDef.getEnumValues().forEach(enumArray::add);
                    propertyNode.set(ENUM, enumArray);
                }
                propertiesNode.set(propName, propertyNode);
            });
            schemaNode.set(PROPERTIES, propertiesNode);
        }

        if (schemaDef.getRequired() != null) {
            ArrayNode requiredArray = mapper.createArrayNode();
            schemaDef.getRequired().forEach(requiredArray::add);
            schemaNode.set(REQUIRED, requiredArray);
        }

        schemas.set(schemaName, schemaNode);
    }

    private boolean hasTag(JsonNode operation, String targetTag) {
        JsonNode tagsNode = operation.get(TAGS);
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tag : tagsNode) {
                if (tag.isTextual() && tag.asText().equals(targetTag)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ObjectNode ensureNode(ObjectNode parent, String fieldName) {
        JsonNode node = parent.get(fieldName);
        if (node == null || !node.isObject()) {
            ObjectNode newNode = mapper.createObjectNode();
            parent.set(fieldName, newNode);
            return newNode;
        }
        return (ObjectNode) node;
    }
}
