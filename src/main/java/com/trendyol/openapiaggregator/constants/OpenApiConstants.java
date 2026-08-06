package com.trendyol.openapiaggregator.constants;

import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Constants for OpenAPI 3.0 specification document structure.
 * These constants represent standard field names used in OpenAPI documents.
 */
@UtilityClass
public final class OpenApiConstants {

    // Root level fields
    public static final String OPENAPI = "openapi";
    public static final String OPENAPI_VERSION_3_0_1 = "3.0.1";
    public static final String INFO = "info";
    public static final String PATHS = "paths";
    public static final String COMPONENTS = "components";
    public static final String TAGS = "tags";
    // Info object fields
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String VERSION = "version";
    // Components object fields
    public static final String SCHEMAS = "schemas";
    public static final String RESPONSES = "responses";
    public static final String PARAMETERS = "parameters";
    public static final String SECURITY_SCHEMES = "securitySchemes";
    // Common fields
    public static final String NAME = "name";
    public static final String SCHEMA = "schema";
    public static final String SCHEMA_REF = "$ref";
    public static final String TYPE = "type";
    public static final String FORMAT = "format";
    public static final String REQUIRED = "required";
    public static final String DEFAULT = "default";
    public static final String ENUM = "enum";
    public static final String PROPERTIES = "properties";
    // Operation object fields
    public static final String OPERATION_ID = "operationId";
    public static final String SECURITY = "security";
    // Parameter object fields
    public static final String PARAMETER_IN = "in";
    public static final String PARAMETER_IN_PATH = "path";
    public static final String PARAMETER_IN_HEADER = "header";
    // Response object fields
    public static final String CONTENT = "content";
    // Media type fields
    public static final String APPLICATION_JSON = "application/json";
    // Security scheme fields
    public static final String SCHEME = "scheme";
    public static final String BEARER_FORMAT = "bearerFormat";
    public static final String BEARER_AUTH = "BearerAuth";
    public static final String HTTP = "http";
    public static final String BEARER = "bearer";
    public static final String JWT = "JWT";
    // Common HTTP methods
    public static final String METHOD_GET = "get";
    public static final String METHOD_POST = "post";
    public static final String METHOD_PUT = "put";
    public static final String METHOD_DELETE = "delete";
    public static final String METHOD_PATCH = "patch";
    public static final String METHOD_OPTIONS = "options";
    public static final String METHOD_HEAD = "head";
    public static final List<String> HTTP_METHODS = List.of(METHOD_GET, METHOD_POST, METHOD_PUT, METHOD_DELETE, METHOD_PATCH, METHOD_OPTIONS, METHOD_HEAD);

}
