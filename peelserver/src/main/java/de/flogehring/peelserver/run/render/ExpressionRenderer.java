package de.flogehring.peelserver.run.render;

import de.flogehring.peel.core.trace.TraceExpressionKind;
import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import io.pebbletemplates.pebble.PebbleEngine;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ExpressionRenderer {

    private final ExpressionRenderConfiguration expressionRenderConfiguration;

    private ExpressionRenderer(ExpressionRenderConfiguration configuration) {
        this.expressionRenderConfiguration = configuration;
    }

    public static ExpressionRenderer ofDefaultRenderers() {
        return of(ExpressionRenderConfiguration.defaultConfig());
    }

    public static ExpressionRenderer of(ExpressionRenderConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        return new ExpressionRenderer(configuration);
    }

    public static boolean supports(Object input) {
        return supportsExpression(input);
    }

    public String render(Object input) {
        return renderExpressionNode(input);
    }

    public static boolean supportsExpression(Object input) {
        if (!(input instanceof Map<?, ?> map)) {
            return false;
        }
        TraceExpressionKind kind = expressionKind(asStringMap(map));
        return kind != null;
    }

    public String renderExpressionNode(Object input) {
        if (!supportsExpression(input)) {
            throw new IllegalArgumentException("Unsupported argument");
        }
        Map<String, Object> expression = asStringMap(input);
        TraceExpressionKind kind = expressionKind(expression);
        if (kind == null) {
            throw new IllegalStateException();
        }
        String template = expressionRenderConfiguration.getTemplates().get(kind);
        String valueText = valueText(expression.get("value"));
        if (template == null || template.isBlank()) {
            return valueText;
        }
        expression.put("valueText", valueText);
        return renderTemplate(template, expression);
    }

    private String renderTemplate(String template, Map<String, Object> context) {
        PebbleEngine engine = new PebbleEngine.Builder()
                .autoEscaping(false)
                .extension(new TraceRenderingPebbleExtension(expressionRenderConfiguration))
                .build();
        try {
            StringWriter stringBuilder = new StringWriter();
            engine.getLiteralTemplate(template).evaluate(stringBuilder, context);
            return stringBuilder.toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to render template: " + ex.getMessage(), ex);
        }
    }

    private static TraceExpressionKind expressionKind(Map<String, Object> expression) {
        if (expression == null) {
            return null;
        }
        String type = asString(expression.get("type"));
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "literal" -> TraceExpressionKind.LITERAL;
            case "binary_operator" -> TraceExpressionKind.BINARY_OPERATOR;
            case "unary_prefix_operator" -> TraceExpressionKind.UNARY_PREFIX_OPERATOR;
            case "variable_name" -> TraceExpressionKind.VARIABLE_NAME;
            case "function_call" -> TraceExpressionKind.FUNCTION_CALL;
            case "return_expression" -> TraceExpressionKind.RETURN_EXPR;
            case "assignment" -> TraceExpressionKind.ASSIGNMENT;
            case "if_statement" -> TraceExpressionKind.IF_STATEMENT;
            case "while_loop" -> TraceExpressionKind.WHILE_LOOP;
            case "for_each_loop" -> TraceExpressionKind.FOR_EACH_LOOP;
            case "list_literal" -> TraceExpressionKind.LIST_LITERAL;
            case "map_literal" -> TraceExpressionKind.MAP_LITERAL;
            case "selector" -> TraceExpressionKind.SELECTOR;
            case "block" -> TraceExpressionKind.BLOCK;
            default -> null;
        };
    }

    private static String valueText(Object valueNode) {
        Map<String, Object> valueMap = asStringMap(valueNode);
        if (valueMap == null) {
            return stringifyValue(valueNode);
        }

        String valueType = asString(valueMap.get("type"));
        if (valueType == null) {
            return stringifyValue(valueMap.get("value"));
        }

        return switch (valueType) {
            case "text" -> String.valueOf(valueMap.get("value"));
            case "none" -> "none";
            case "list" -> {
                List<?> items = asList(valueMap.get("items"));
                List<String> renderedItems = items.stream().map(ExpressionRenderer::valueText).toList();
                yield "[" + String.join(", ", renderedItems) + "]";
            }
            case "map" -> {
                List<?> entries = asList(valueMap.get("entries"));
                List<String> renderedEntries = new ArrayList<>();
                for (Object entryObject : entries) {
                    Map<String, Object> entry = asStringMap(entryObject);
                    if (entry == null) {
                        continue;
                    }
                    renderedEntries.add(valueText(entry.get("key")) + ": " + valueText(entry.get("value")));
                }
                yield "{" + String.join(", ", renderedEntries) + "}";
            }
            case "callable_ref" -> {
                String kind = asString(valueMap.get("callableKind"));
                String name = asString(valueMap.get("name"));
                Object arities = valueMap.get("arities");
                yield (kind == null ? "callable" : kind)
                        + " "
                        + (name == null ? "<name>" : name)
                        + "/"
                        + stringifyValue(arities);
            }
            default -> stringifyValue(valueMap.get("value"));
        };
    }

    private static String stringifyValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof List<?> listValue) {
            List<String> rendered = listValue.stream().map(ExpressionRenderer::stringifyValue).toList();
            return "[" + String.join(", ", rendered) + "]";
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> converted = asStringMap(mapValue);
            if (converted == null) {
                return String.valueOf(value);
            }
            List<String> entries = converted.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + stringifyValue(entry.getValue()))
                    .toList();
            return "{" + String.join(", ", entries) + "}";
        }
        return String.valueOf(value);
    }

    private static List<?> asList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    private static String asString(Object value) {
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static Map<String, Object> asStringMap(Object node) {
        if (!(node instanceof Map<?, ?> map)) {
            return null;
        }
        LinkedHashMap<String, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                return null;
            }
            converted.put(key, entry.getValue());
        }
        return converted;
    }
}
