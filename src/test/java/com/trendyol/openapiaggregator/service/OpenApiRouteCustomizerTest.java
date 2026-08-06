package com.trendyol.openapiaggregator.service;

import com.trendyol.openapiaggregator.configuration.OpenApiFilterConfig;
import com.trendyol.openapiaggregator.configuration.OpenApiInfoConfig;
import com.trendyol.openapiaggregator.configuration.ResponseCustomizationConfig;
import com.trendyol.openapiaggregator.dto.ServiceApiDoc;
import com.trendyol.openapiaggregator.transformers.OpenApiFilterTransformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.API_ITEMS_PATH;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.BEARER;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.CHALLENGE_ID;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.CHALLENGE_RESPONSE;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.COMPONENTS;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.CONFIGURED_TITLE;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.FIRST_SCHEMA;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.FIRST_SECURITY_SCHEME;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.FIRST_SERVICE;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.GET;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.GET_OPERATION_POINTER;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.INFO;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.LEGACY_SCHEMA;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.LEGACY_SERVICE;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.OPENAPI;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.OPENAPI_VERSION;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.OPERATION_CREATE;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.OPERATION_LIST;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.PATHS;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.PATH_PATTERN;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.POST_OPERATION_POINTER;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.REQUIRED;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.RESPONSE_SCHEMA_POINTER;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.SCHEMAS;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.SECOND_SCHEMA;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.SECOND_SECURITY_SCHEME;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.SECOND_SERVICE;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.SECURITY_SCHEMES;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.SERVICE;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.TITLE;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.UNIFIED_TITLE;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.WANTED_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenApiRouteCustomizerTest {
    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private RouteDefinitionAccessor routes;

    @Spy
    private OpenApiFilterConfig filters = new OpenApiFilterConfig();

    @Spy
    private ResponseCustomizationConfig responses = new ResponseCustomizationConfig();

    @Spy
    private OpenApiInfoConfig info = new OpenApiInfoConfig();

    @Mock
    private List<OpenApiFilterTransformer> transformers;

    @InjectMocks
    private OpenApiRouteCustomizer customizer;

    @BeforeEach
    void setUp() {
        lenient().when(routes.getPredicates(anyString())).thenReturn(Mono.just(Map.of()));
        lenient().when(routes.getFilterConfigs(anyString())).thenReturn(Mono.just(Map.of()));
        lenient().when(transformers.stream()).thenReturn(Stream.of());
    }

    @Test
    void appliesTagPathAndMethodFiltersAndAddsConfiguredResponseModel() throws Exception {
        String unauthorizedStatusCode = "401";
        String challengeDescription = "Challenge required";
        OpenApiFilterConfig.ServiceFilter filter = new OpenApiFilterConfig.ServiceFilter();
        filter.setIncludeTags(List.of(WANTED_TAG));
        filters.getFilters().put(SERVICE, filter);
        when(routes.getPredicates(SERVICE)).thenReturn(Mono.just(Map.of(
                "Path", Map.of("pattern", PATH_PATTERN), "Method", Map.of("_genkey_0", GET))));

        ResponseCustomizationConfig.CustomResponse response = new ResponseCustomizationConfig.CustomResponse();
        response.setStatusCode(unauthorizedStatusCode);
        response.setDescription(challengeDescription);
        response.setSchemaRef("#/components/schemas/" + CHALLENGE_RESPONSE);

        ResponseCustomizationConfig.SchemaDefinition schema = new ResponseCustomizationConfig.SchemaDefinition();
        schema.setType("object");
        schema.setRequired(List.of(CHALLENGE_ID));
        response.setInlineSchema(schema);
        responses.getTagResponses().put(WANTED_TAG, List.of(response));

        JsonNode document = mapper.readTree(customizer.applyOpenApiCustomisations(new ServiceApiDoc(SERVICE, """
                {"openapi":"3.0.1","info":{},"paths":{
                  "/api/items":{"get":{"tags":["wanted"],"responses":{}}},
                  "/api/write":{"post":{"tags":["wanted"],"responses":{}}},
                  "/other":{"get":{"tags":["wanted"],"responses":{}}},
                  "/api/hidden":{"get":{"tags":["other"],"responses":{}}}}}
                """)).block());

        String pathsField = PATHS;
        String responseDescriptionPath = "/paths/~1api~1items/get/responses/" + unauthorizedStatusCode + "/description";
        String responseDescription = document.at(responseDescriptionPath).asText();
        String schemaRequired = document.path(COMPONENTS).path(SCHEMAS).path(CHALLENGE_RESPONSE)
                .path(REQUIRED).toString();

        assertThat(document.path(pathsField).fieldNames()).toIterable().containsExactly(API_ITEMS_PATH);
        assertThat(responseDescription).isEqualTo(challengeDescription);
        assertThat(schemaRequired).isEqualTo("[\"" + CHALLENGE_ID + "\"]");
    }

    @Test
    void convertsSwagger2DocumentsToOpenApi3() throws Exception {
        JsonNode document = mapper.readTree(customizer.applyOpenApiCustomisations(new ServiceApiDoc(LEGACY_SERVICE, """
                {"swagger":"2.0","info":{},"definitions":{"Legacy":{"type":"object"}},
                 "securityDefinitions":{"Bearer":{"type":"apiKey","name":"Authorization","in":"header"}},
                 "paths":{"/items":{"get":{"responses":{"200":{"description":"OK",
                   "schema":{"$ref":"#/definitions/Legacy"}}}}}}}
                """)).block());

        String openApiVersion = document.path(OPENAPI).asText();
        boolean hasLegacySchema = document.path(COMPONENTS).path(SCHEMAS).has(LEGACY_SCHEMA);
        boolean hasBearerScheme = document.path(COMPONENTS).path(SECURITY_SCHEMES).has(BEARER);
        String legacySchemaReference = document.at(RESPONSE_SCHEMA_POINTER).asText();

        assertThat(openApiVersion).isEqualTo(OPENAPI_VERSION);
        assertThat(hasLegacySchema).isTrue();
        assertThat(hasBearerScheme).isTrue();
        assertThat(legacySchemaReference).isEqualTo("#/components/schemas/" + LEGACY_SCHEMA);
    }

    @Test
    void mergesPathsSchemasSecurityAndPrefixesOperationIds() throws Exception {
        JsonNode document = mapper.readTree(customizer.mergeApiDocs(List.of(
                new ServiceApiDoc(FIRST_SERVICE, """
                        {"openapi":"3.0.1","info":{},"paths":{"/items":{"get":{"operationId":"list","responses":{}}}},
                         "components":{"schemas":{"First":{"type":"object"}},"securitySchemes":{"A":{"type":"apiKey"}}},"tags":[{"name":"first"}]}
                        """),
                new ServiceApiDoc(SECOND_SERVICE, """
                        {"openapi":"3.0.1","info":{},"paths":{"/items":{"post":{"operationId":"create","responses":{}}},"/other":{"get":{"operationId":"other","responses":{}}}},
                         "components":{"schemas":{"Second":{"type":"object"}},"securitySchemes":{"B":{"type":"http"}}},"tags":[{"name":"second"}]}
                        """)
        )).block());

        String firstOperationId = document.at(GET_OPERATION_POINTER).asText();
        String secondOperationId = document.at(POST_OPERATION_POINTER).asText();
        List<String> schemaNames = new ArrayList<>();
        document.path(COMPONENTS).path(SCHEMAS).fieldNames().forEachRemaining(schemaNames::add);
        List<String> securitySchemeNames = new ArrayList<>();
        document.path(COMPONENTS).path(SECURITY_SCHEMES).fieldNames().forEachRemaining(securitySchemeNames::add);
        String unifiedTitle = document.path(INFO).path(TITLE).asText();

        assertThat(firstOperationId).isEqualTo(OPERATION_LIST);
        assertThat(secondOperationId).isEqualTo(OPERATION_CREATE);
        assertThat(schemaNames).contains(FIRST_SCHEMA, SECOND_SCHEMA);
        assertThat(securitySchemeNames).contains(FIRST_SECURITY_SCHEME, SECOND_SECURITY_SCHEME);
        assertThat(unifiedTitle).isEqualTo(UNIFIED_TITLE);
    }

    @Test
    void createsConfiguredEmptySpecWhenNoServicesExist() throws Exception {
        info.setTitle(CONFIGURED_TITLE);
        JsonNode document = mapper.readTree(customizer.mergeApiDocs(List.of()).block());

        String openApiVersion = document.path(OPENAPI).asText();
        String configuredTitle = document.path(INFO).path(TITLE).asText();
        JsonNode paths = document.path(PATHS);

        assertThat(openApiVersion).isEqualTo(OPENAPI_VERSION);
        assertThat(configuredTitle).isEqualTo(CONFIGURED_TITLE);
        assertThat(paths).isEmpty();
    }
}
