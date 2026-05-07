package de.flogehring.peelserver.documents;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ScriptTraceLineFormatter {

    public List<String> formatProgramLines(Map<String, Object> traceProgram) {
        Objects.requireNonNull(traceProgram, "traceProgram");
        Object expressions = traceProgram.get("expressions");
        if (!(expressions instanceof List<?> expressionList)) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        for (Object expression : expressionList) {
            if (!(expression instanceof Map<?, ?> expressionMap)) {
                continue;
            }
            String line = formatTopLevelExpression(expressionMap);
            if (line != null && !line.isBlank()) {
                lines.add(line);
            }
        }
        return Collections.unmodifiableList(lines);
    }

    private String formatTopLevelExpression(Map<?, ?> expressionMap) {
        String type = asString(expressionMap.get("type"));
        if ("assignment".equals(type)) {
            String variableName = asString(expressionMap.get("variableName"));
            Object rhs = expressionMap.get("expression");
            String rhsText = formatExpression(rhs);
            if (variableName == null || variableName.isBlank()) {
                return rhsText + ";";
            }
            return "var " + variableName + " = " + rhsText + ";";
        }
        return formatExpression(expressionMap);
    }

    private String formatExpression(Object node) {
        if (!(node instanceof Map<?, ?> expressionMap)) {
            return "<unknown>";
        }

        String type = asString(expressionMap.get("type"));
        if (type == null) {
            return valueText(expressionMap.get("value"));
        }

        switch (type) {
            case "binary_operator": {
                String operator = asString(expressionMap.get("operator"));
                String lhs = formatExpression(expressionMap.get("lhs"));
                String rhs = formatExpression(expressionMap.get("rhs"));
                if (operator == null) {
                    operator = "?";
                }
                return lhs + " " + operator + " " + rhs;
            }
            case "unary_prefix_operator": {
                String operator = asString(expressionMap.get("operator"));
                String argument = formatExpression(expressionMap.get("argument"));
                return (operator == null ? "" : operator) + argument;
            }
            case "variable_name":
                return valueText(expressionMap.get("value"));
            case "literal":
                return valueText(expressionMap.get("value"));
            case "assignment": {
                String variableName = asString(expressionMap.get("variableName"));
                String rhs = formatExpression(expressionMap.get("expression"));
                if (variableName == null || variableName.isBlank()) {
                    return rhs;
                }
                return variableName + " = " + rhs;
            }
            case "return_expression":
                return formatExpression(expressionMap.get("expression"));
            case "selector": {
                String target = formatExpression(expressionMap.get("target"));
                String selector = formatExpression(expressionMap.get("selector"));
                return target + "." + selector;
            }
            case "function_call": {
                String name = asString(expressionMap.get("name"));
                List<String> argTexts = new ArrayList<>();
                Object args = expressionMap.get("arguments");
                if (args instanceof List<?> argumentList) {
                    for (Object argument : argumentList) {
                        argTexts.add(formatExpression(argument));
                    }
                }
                return (name == null ? "<call>" : name) + "(" + String.join(", ", argTexts) + ")";
            }
            default:
                return valueText(expressionMap.get("value"));
        }
    }

    private String valueText(Object valueNode) {
        if (!(valueNode instanceof Map<?, ?> valueMap)) {
            return "<unknown>";
        }

        String valueType = asString(valueMap.get("type"));
        Object value = valueMap.get("value");

        if (valueType == null) {
            return stringifyValue(value);
        }

        switch (valueType) {
            case "text":
                return quote(value);
            case "none":
                return "none";
            case "list": {
                Object items = valueMap.get("items");
                if (!(items instanceof List<?> itemList)) {
                    return "[]";
                }
                List<String> values = new ArrayList<>();
                for (Object item : itemList) {
                    values.add(valueText(item));
                }
                return "[" + String.join(", ", values) + "]";
            }
            case "map": {
                Object entries = valueMap.get("entries");
                if (!(entries instanceof List<?> entryList)) {
                    return "{}";
                }
                List<String> renderedEntries = new ArrayList<>();
                for (Object entry : entryList) {
                    if (!(entry instanceof Map<?, ?> entryMap)) {
                        continue;
                    }
                    String key = valueText(entryMap.get("key"));
                    String val = valueText(entryMap.get("value"));
                    renderedEntries.add(key + ": " + val);
                }
                return "{" + String.join(", ", renderedEntries) + "}";
            }
            default:
                return stringifyValue(value);
        }
    }

    private String stringifyValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> mapValue) {
            return mapValueToString(mapValue);
        }
        if (value instanceof List<?> listValue) {
            List<String> rendered = listValue.stream().map(this::stringifyValue).toList();
            return "[" + String.join(", ", rendered) + "]";
        }
        return String.valueOf(value);
    }

    private String mapValueToString(Map<?, ?> mapValue) {
        LinkedHashMap<String, String> rendered = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
            rendered.put(String.valueOf(entry.getKey()), stringifyValue(entry.getValue()));
        }
        List<String> pairs = rendered.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .toList();
        return "{" + String.join(", ", pairs) + "}";
    }

    private String quote(Object value) {
        return '"' + (value == null ? "" : String.valueOf(value)) + '"';
    }

    private String asString(Object value) {
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
