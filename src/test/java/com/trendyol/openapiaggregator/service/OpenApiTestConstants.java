package com.trendyol.openapiaggregator.service;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class OpenApiTestConstants {
    public static final String SERVICE = "service";
    public static final String LEGACY_SERVICE = "legacy";
    public static final String FIRST_SERVICE = "first";
    public static final String SECOND_SERVICE = "second";
    public static final String WANTED_TAG = "wanted";
    public static final String CHALLENGE_RESPONSE = "ChallengeResponse";
    public static final String CHALLENGE_ID = "challengeId";
    public static final String LEGACY_SCHEMA = "Legacy";
    public static final String BEARER = "Bearer";
    public static final String OPENAPI_VERSION = "3.0.1";
    public static final String GET = "GET";
    public static final String PATH_PATTERN = "/api/**";
    public static final String API_ITEMS_PATH = "/api/items";
    public static final String CONFIGURED_TITLE = "Configured title";
    public static final String COMPONENTS = "components";
    public static final String OBJECT_VALUE = "value";
    public static final String ORDERS = "orders";
    public static final String ORDERS_URI = "https://orders.example";
    public static final String DOCS_PATH = "/docs";
    public static final String CUSTOMIZED = "customized";
    public static final String EMPTY = "empty";
    public static final String MISSING = "missing";
    public static final String TEST_FILTER = "test";
    public static final String NOT_OBJECT = "not an object";
    public static final String OPENAPI = "openapi";
    public static final String INFO = "info";
    public static final String TITLE = "title";
    public static final String PATHS = "paths";
    public static final String SCHEMAS = "schemas";
    public static final String SECURITY_SCHEMES = "securitySchemes";
    public static final String REQUIRED = "required";
    public static final String OPERATION_LIST = "list";
    public static final String OPERATION_CREATE = "second_create";
    public static final String UNIFIED_TITLE = "Unified Gateway API";
    public static final String FIRST_SCHEMA = "First";
    public static final String SECOND_SCHEMA = "Second";
    public static final String FIRST_SECURITY_SCHEME = "A";
    public static final String SECOND_SECURITY_SCHEME = "B";
    public static final String RESPONSE_SCHEMA_POINTER = "/paths/~1items/get/responses/200/content/application~1json/schema/$ref";
    public static final String GET_OPERATION_POINTER = "/paths/~1items/get/operationId";
    public static final String POST_OPERATION_POINTER = "/paths/~1items/post/operationId";

}
