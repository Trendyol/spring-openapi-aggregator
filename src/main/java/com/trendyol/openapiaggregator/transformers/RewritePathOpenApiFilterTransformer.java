package com.trendyol.openapiaggregator.transformers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms paths for Spring Cloud Gateway's {@code RewritePath} filter.
 * <p>
 * {@code RewritePath} maps a public request path to a backend path using a
 * regular expression and replacement. This transformer applies the inverse
 * mapping to service OpenAPI paths so the documentation exposes the public
 * gateway path, including named path variables where possible.
 *
 * @see <a href="https://docs.spring.io/spring-cloud-gateway/docs/4.1.x/reference/html/#the-rewritepath-gatewayfilter-factory">RewritePath GatewayFilter Factory</a>
 */
@RequiredArgsConstructor
public class RewritePathOpenApiFilterTransformer implements OpenApiFilterTransformer {

    private static String invert(String backendPath, Pattern incomingPattern, String replacement) {
        // For the normal Gateway use case, the replacement contains named
        // groups (e.g. /${segment}); reconstruct the incoming path from them.
        try {
            java.util.List<String> names = groupNames(replacement);
            if (names.isEmpty()) {
                return backendPath;
            }
            StringBuilder expression = new StringBuilder("^");
            int cursor = 0;
            Matcher placeholders = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9]*)}").matcher(replacement);
            while (placeholders.find()) {
                expression.append(Pattern.quote(replacement.substring(cursor, placeholders.start())));
                expression.append("(?<").append(placeholders.group(1)).append(">.*)");
                cursor = placeholders.end();
            }
            expression.append(Pattern.quote(replacement.substring(cursor))).append("$");
            Matcher inverse = Pattern.compile(expression.toString()).matcher(backendPath);
            if (!inverse.matches()) {
                return backendPath;
            }
            String result = incomingPattern.pattern();
            for (String name : names) {
                String group = "(?<" + name + ">";
                int start = result.indexOf(group);
                if (start < 0) {
                    continue;
                }
                int end = result.indexOf(')', start);
                if (end >= 0) {
                    result = result.substring(0, start) + inverse.group(name) + result.substring(end + 1);
                }
            }
            return result;
        } catch (RuntimeException ignored) {
            return backendPath;
        }
    }

    private static java.util.List<String> groupNames(String regex) {
        java.util.List<String> names = new java.util.ArrayList<>();
        String groupPattern = regex.contains("${")
                ? "\\$\\{([A-Za-z][A-Za-z0-9]*)}"
                : "\\(\\?<([A-Za-z][A-Za-z0-9]*)>";
        Matcher matcher = Pattern.compile(groupPattern).matcher(regex);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    @Override public String getFilterName() { return "RewritePath"; }

    @Override public void transform(ObjectNode root, Map<String, String> args) {
        String regexp = args.getOrDefault("regexp", args.get("_genkey_0"));
        String replacement = args.getOrDefault("replacement", args.get("_genkey_1"));
        if (regexp == null || replacement == null) {
            return;
        }
        Pattern pattern;
        try { pattern = Pattern.compile(regexp); } catch (RuntimeException ignored) { return; }
        PathTransformerSupport.transform(root, path -> invert(path, pattern, replacement));
    }
}
