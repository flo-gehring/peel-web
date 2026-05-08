package de.flogehring.peelserver.run.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DefaultTraceExpressionRenderers {

    private static final Set<String> TRACE_EXPRESSION_TYPES = Set.of(
            "literal",
            "binary_operator",
            "unary_prefix_operator",
            "variable_name",
            "function_call",
            "return_expression",
            "assignment",
            "if_statement",
            "while_loop",
            "for_each_loop",
            "list_literal",
            "map_literal",
            "selector",
            "block"
    );

    private DefaultTraceExpressionRenderers() {
    }

    public static boolean supports(Object input) {
        if (!(input instanceof Map<?, ?> map)) {
            return false;
        }
        Object type = map.get("type");
        return type instanceof String typeName && TRACE_EXPRESSION_TYPES.contains(typeName);
    }

    public static String render(Object input, boolean showValue) {
        if (!(input instanceof Map<?, ?> map)) {
            return String.valueOf(input);
        }
        Map<String, Object> expression = asStringMap(map);
        if (expression == null) {
            return String.valueOf(input);
        }
        if (!supports(expression)) {
            return String.valueOf(input);
        }
        return renderExpression(expression, showValue);
    }

    private static String renderExpression(Map<String, Object> expression, boolean showValue) {
        String type = asString(expression.get("type"));
        if (type == null) {
            return valueText(expression.get("value"));
        }

        String rendered = switch (type) {
            case "literal" -> valueText(expression.get("value"));
            case "binary_operator" -> {
                String operator = asString(expression.get("operator"));
                String lhs = renderExpressionOrValue(expression.get("lhs"), showValue);
                Object rhsObject = expression.get("rhs");
                String rhs = rhsObject == null ? "<short-circuit>" : renderExpressionOrValue(rhsObject, showValue);
                yield lhs + " " + (operator == null ? "?" : operator) + " " + rhs;
            }
            case "unary_prefix_operator" -> {
                String operator = asString(expression.get("operator"));
                String argument = renderExpressionOrValue(expression.get("argument"), showValue);
                yield (operator == null ? "" : operator) + argument;
            }
            case "variable_name" -> {
                String name = asString(expression.get("name"));
                yield name == null || name.isBlank() ? valueText(expression.get("value")) : name;
            }
            case "function_call" -> {
                String name = asString(expression.get("name"));
                List<?> arguments = asList(expression.get("arguments"));
                List<String> renderedArguments = arguments.stream()
                        .map(argument -> renderExpressionOrValue(argument, showValue))
                        .toList();
                yield (name == null ? "<call>" : name) + "(" + String.join(", ", renderedArguments) + ")";
            }
            case "return_expression" -> "return " + renderExpressionOrValue(expression.get("expression"), showValue);
            case "assignment" -> {
                String variableName = asString(expression.get("variableName"));
                String rhs = renderExpressionOrValue(expression.get("expression"), showValue);
                yield (variableName == null ? "<var>" : variableName) + " = " + rhs;
            }
            case "if_statement" -> {
                List<?> conditions = asList(expression.get("conditions"));
                List<String> renderedConditions = conditions.stream()
                        .map(condition -> renderExpressionOrValue(condition, showValue))
                        .toList();
                Object executedBlock = expression.get("executedBlock");
                String blockText = executedBlock == null ? "{ }" : renderExpressionOrValue(executedBlock, showValue);
                yield "if (" + String.join(", ", renderedConditions) + ") " + blockText;
            }
            case "while_loop" -> {
                List<?> iterations = asList(expression.get("iterations"));
                yield "while (...) { ... } // iterations=" + iterations.size();
            }
            case "for_each_loop" -> {
                String variableName = asString(expression.get("variableName"));
                String iterable = renderExpressionOrValue(expression.get("iterableExpression"), showValue);
                List<?> iterations = asList(expression.get("iterations"));
                yield "for (" + (variableName == null ? "_" : variableName) + " in " + iterable + ") { ... }"
                        + " // iterations=" + iterations.size();
            }
            case "list_literal" -> {
                List<?> elements = asList(expression.get("elements"));
                List<String> renderedElements = elements.stream()
                        .map(element -> renderExpressionOrValue(element, showValue))
                        .toList();
                yield "[" + String.join(", ", renderedElements) + "]";
            }
            case "map_literal" -> {
                List<?> entries = asList(expression.get("entries"));
                List<String> renderedEntries = new ArrayList<>();
                for (Object entryObject : entries) {
                    Map<String, Object> entry = asStringMap(entryObject);
                    if (entry == null) {
                        continue;
                    }
                    renderedEntries.add(
                            renderExpressionOrValue(entry.get("key"), showValue)
                                    + ": "
                                    + renderExpressionOrValue(entry.get("value"), showValue)
                    );
                }
                yield "{" + String.join(", ", renderedEntries) + "}";
            }
            case "selector" -> {
                String target = renderExpressionOrValue(expression.get("target"), showValue);
                String selector = renderExpressionOrValue(expression.get("selector"), showValue);
                yield target + "[" + selector + "]";
            }
            case "block" -> {
                List<?> content = asList(expression.get("content"));
                List<String> renderedContent = content.stream()
                        .map(item -> renderExpression((Map<String, Object>) item, showValue))
                        .toList();
                yield "{ " + String.join("; ", renderedContent) + " }";
            }
            default -> valueText(expression.get("value"));
        };

        if (!showValue) {
            return rendered;
        }
        if (!expression.containsKey("value")) {
            return rendered;
        }
        return rendered + " => " + valueText(expression.get("value"));
    }

    private static String renderExpressionOrValue(Object value, boolean showValue) {
        if (supports(value)) {
            return render(value, showValue);
        }
        return valueText(value);
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
            case "text" -> quote(valueMap.get("value"));
            case "none" -> "none";
            case "list" -> {
                List<?> items = asList(valueMap.get("items"));
                List<String> renderedItems = items.stream().map(DefaultTraceExpressionRenderers::valueText).toList();
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

    private static String quote(Object value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + String.valueOf(value) + "\"";
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
            List<String> rendered = listValue.stream().map(DefaultTraceExpressionRenderers::stringifyValue).toList();
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

    @SuppressWarnings("unchecked")
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
