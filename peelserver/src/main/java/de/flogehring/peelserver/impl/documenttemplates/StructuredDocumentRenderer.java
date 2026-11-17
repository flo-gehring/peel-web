package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.api.data.template.DocumentNode;
import de.flogehring.peelserver.impl.run.render.ExpressionRenderer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StructuredDocumentRenderer {
    private final TraceSelection selection;
    private final ExpressionRenderer expressions;
    private final List<String> warnings = new ArrayList<>();

    StructuredDocumentRenderer(TraceSelection selection, ExpressionRenderer expressions) {
        this.selection = selection;
        this.expressions = expressions;
    }

    List<String> warnings() {
        return List.copyOf(warnings);
    }

    String render(DocumentNode node) {
        return render(node, null, "document");
    }

    private String render(DocumentNode node, TraceSelection.Occurrence current, String path) {
        try {
            String type = node.type();
            if (type.equals("peelTable")) return table(node, current, path);
            if (type.equals("peelRepeat")) {
                var occurrences = select(node, current);
                if (occurrences.isEmpty())
                    warnings.add(path + ": block '" + node.attribute("block") + "' was not executed");
                StringBuilder html = new StringBuilder();
                for (var occurrence : occurrences)
                    html.append("<div class=\"peel-repeat\">")
                            .append(children(node, occurrence, path)).append("</div>");
                return html.toString();
            }
            if (type.equals("peelCalculation") || type.equals("peelValue")) {
                var occurrences = select(node, current);
                if (occurrences.size() != 1)
                    throw new IllegalArgumentException("Expected one trace occurrence, found " + occurrences.size());
                var occurrence = occurrences.getFirst();
                if (type.equals("peelValue")) {
                    Object value = occurrence.node().get("value");
                    if (!node.attribute("binding").isBlank()) value = binding(occurrence, node.attribute("binding"));
                    return escape(valueText(value, decimals(node)));
                }
                return "<div class=\"peel-calculation\">" + calculation(occurrence.node(), node.attribute("mode"), decimals(node)) + "</div>";
            }
            if (type.equals("text")) {
                String text = escape(node.text());
                for (var mark : node.marks()) {
                    String tag = switch (mark.type()) {
                        case "bold" -> "strong";
                        case "italic" -> "em";
                        case "strike" -> "s";
                        case "underline" -> "u";
                        case "code" -> "code";
                        default -> "span";
                    };
                    if (mark.type().equals("link") && mark.attrs() != null) {
                        String href = String.valueOf(mark.attrs().getOrDefault("href", ""));
                        if (href.matches("(?i)^(https?://|mailto:|#).*")) {
                            text = "<a href=\"" + escape(href) + "\">" + text + "</a>";
                            continue;
                        }
                    }
                    text = "<" + tag + ">" + text + "</" + tag + ">";
                }
                return text;
            }
            if (type.equals("hardBreak")) return "<br />";
            if (type.equals("horizontalRule")) return "<hr />";
            String inner = children(node, current, path);
            String tag = switch (type) {
                case "doc" -> "div";
                case "paragraph" -> "p";
                case "heading" -> "h" + heading(node);
                case "bulletList" -> "ul";
                case "orderedList" -> "ol";
                case "listItem" -> "li";
                case "blockquote" -> "blockquote";
                case "codeBlock" -> "pre";
                default -> throw new IllegalArgumentException("Unsupported node: " + type);
            };
            String attributes = type.equals("orderedList") ? " start=\"" + start(node) + "\"" : "";
            return "<" + tag + attributes + ">" + inner + "</" + tag + ">";
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(path + " (" + node.type() + "): " + exception.getMessage(), exception);
        }
    }

    private List<TraceSelection.Occurrence> select(DocumentNode node, TraceSelection.Occurrence current) {
        return selection.select(node.attribute("scriptId"), node.attribute("block"), current, node.attribute("scope").equals("current"));
    }

    private String table(DocumentNode node, TraceSelection.Occurrence current, String path) {
        var occurrences = select(node, current);
        if (occurrences.isEmpty()) {
            warnings.add(path + ": block '" + node.attribute("block") + "' was not executed");
            return "";
        }
        var html = new StringBuilder("<table class=\"peel-table\"><thead><tr>");
        for (var column : node.content())
            html.append("<th scope=\"col\" style=\"text-align:")
                    .append(alignment(column)).append("\">").append(escape(column.attribute("title"))).append("</th>");
        html.append("</tr></thead><tbody>");
        for (int row = 0; row < occurrences.size(); row++) {
            html.append("<tr>");
            for (int col = 0; col < node.content().size(); col++) {
                var column = node.content().get(col);
                html.append("<td style=\"text-align:").append(alignment(column)).append("\">")
                        .append(children(column, occurrences.get(row), path + "/row/" + row + "/column/" + col)).append("</td>");
            }
            html.append("</tr>");
        }
        return html.append("</tbody></table>").toString();
    }

    private static String alignment(DocumentNode column) {
        return column.attribute("align").isBlank() ? "left" : column.attribute("align");
    }

    private String children(DocumentNode node, TraceSelection.Occurrence current, String path) {
        var html = new StringBuilder();
        for (int i = 0; i < node.content().size(); i++)
            html.append(render(node.content().get(i), current, path + "/" + i));
        return html.toString();
    }

    private static int heading(DocumentNode node) {
        Object level = node.attrs().get("level");
        return level instanceof Number number ? Math.clamp(number.intValue(), 1, 6) : 1;
    }

    private static int start(DocumentNode node) {
        Object start = node.attrs().get("start");
        return start instanceof Number number ? Math.max(1, number.intValue()) : 1;
    }

    private static Integer decimals(DocumentNode node) {
        Object value = node.attrs().get("decimals");
        if (value == null || value.toString().isBlank()) return null;
        try {
            int decimals = Integer.parseInt(value.toString());
            if (decimals < 0 || decimals > 12) throw new NumberFormatException();
            return decimals;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Decimal places must be between 0 and 12");
        }
    }

    private static Object binding(TraceSelection.Occurrence occurrence, String path) {
        String[] parts = path.split("\\.");
        Object value = occurrence.variablesAtExit().get(parts[0]);
        if (value == null) throw new IllegalArgumentException("Unknown variable at block exit: " + parts[0]);
        for (int i = 1; i < parts.length; i++) {
            Map<String, Object> map = map(value);
            if (!"map".equals(map.get("type")))
                throw new IllegalArgumentException("Binding path is not a map: " + path);
            String key = parts[i];
            value = list(map.get("entries")).stream().map(StructuredDocumentRenderer::map)
                    .filter(entry -> key.equals(valueText(entry.get("key"), null)))
                    .map(entry -> entry.get("value")).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown binding field: " + path));
        }
        return value;
    }

    private String calculation(Map<String, Object> expression, String mode, Integer decimals) {
        if (mode.equals("result")) return "<p>" + escape(valueText(expression.get("value"), decimals)) + "</p>";
        String type = String.valueOf(expression.get("type"));
        if (type.equals("block")) return list(expression.get("content")).stream().map(StructuredDocumentRenderer::map)
                .map(child -> calculation(child, mode, decimals)).reduce("", String::concat);
        if (type.equals("if_statement")) {
            StringBuilder html = new StringBuilder();
            for (Object condition : list(expression.get("conditions"))) {
                Map<String, Object> item = map(condition);
                html.append("<p class=\"peel-condition\">if ").append(expressionText(item, mode, decimals)).append(" → ")
                        .append(escape(valueText(item.get("value"), null))).append("</p>");
            }
            if (expression.get("executedBlock") != null)
                html.append(calculation(map(expression.get("executedBlock")), mode, decimals));
            return html.toString();
        }
        if (type.equals("for_each_loop") || type.equals("while_loop")) {
            StringBuilder html = new StringBuilder();
            for (Object item : list(expression.get("iterations"))) {
                Map<String, Object> iteration = map(item);
                if (iteration.get("binding") != null) {
                    var binding = map(iteration.get("binding"));
                    html.append("<p class=\"peel-iteration\">").append(escape(String.valueOf(binding.get("name"))))
                            .append(" = ").append(escape(valueText(binding.get("value"), decimals))).append("</p>");
                } else if (iteration.get("condition") != null) {
                    var condition = map(iteration.get("condition"));
                    html.append("<p class=\"peel-condition\">while ").append(expressionText(condition, mode, decimals)).append(" → ")
                            .append(escape(valueText(condition.get("value"), null))).append("</p>");
                }
                if (iteration.get("body") != null) html.append(calculation(map(iteration.get("body")), mode, decimals));
            }
            return html.toString();
        }
        String html = "<p class=\"peel-equation\">" + expressionText(expression, mode, decimals);
        if (mode.equals("detailed")) {
            Map<String, Object> rhs = Set.of("assignment", "return_expression").contains(type) ? map(expression.get("expression")) : expression;
            if (Set.of("binary_operator", "unary_prefix_operator", "function_call", "selector").contains(rhs.get("type")))
                html += " = " + escape(substituted(rhs, decimals));
        }
        boolean literalAssignment = type.equals("assignment") && "literal".equals(map(expression.get("expression")).get("type"));
        Map<String, Object> rhs = Set.of("assignment", "return_expression").contains(type) ? map(expression.get("expression")) : expression;
        boolean showResult = mode.equals("substituted")
                ? Set.of("binary_operator", "unary_prefix_operator", "function_call").contains(rhs.get("type"))
                : !type.equals("literal") && !literalAssignment;
        if (showResult) html += " = " + escape(valueText(expression.get("value"), decimals));
        return html + "</p>" + (Set.of("detailed", "substituted").contains(mode) ? functionDetails(expression, mode, decimals) : "");
    }

    private String expressionText(Map<String, Object> expression, String mode, Integer decimals) {
        if (!mode.equals("substituted")) return expressions.render(expression);
        String text = switch (String.valueOf(expression.get("type"))) {
            case "assignment" ->
                    expression.get("variableName") + " = " + substituted(map(expression.get("expression")), decimals);
            case "return_expression" -> "return " + substituted(map(expression.get("expression")), decimals);
            default -> substituted(expression, decimals);
        };
        return escape(text);
    }

    private String functionDetails(Map<String, Object> expression, String mode, Integer decimals) {
        StringBuilder html = new StringBuilder();
        switch (String.valueOf(expression.get("type"))) {
            case "assignment", "return_expression" ->
                    html.append(functionDetails(map(expression.get("expression")), mode, decimals));
            case "binary_operator" -> {
                html.append(functionDetails(map(expression.get("lhs")), mode, decimals));
                html.append(functionDetails(map(expression.get("rhs")), mode, decimals));
            }
            case "unary_prefix_operator" ->
                    html.append(functionDetails(map(expression.get("argument")), mode, decimals));
            case "selector" -> {
                html.append(functionDetails(map(expression.get("target")), mode, decimals));
                html.append(functionDetails(map(expression.get("selector")), mode, decimals));
            }
            case "list_literal" ->
                    list(expression.get("elements")).forEach(item -> html.append(functionDetails(map(item), mode, decimals)));
            case "map_literal" -> list(expression.get("entries")).forEach(item -> {
                html.append(functionDetails(map(map(item).get("key")), mode, decimals));
                html.append(functionDetails(map(map(item).get("value")), mode, decimals));
            });
            case "function_call" -> {
                html.append(functionDetails(map(map(expression.get("calleeSource")).get("expression")), mode, decimals));
                list(expression.get("arguments")).forEach(item -> html.append(functionDetails(map(item), mode, decimals)));
                Object body = map(expression.get("subEvaluation")).get("bodyEvaluation");
                if (body != null) html.append("<div class=\"peel-function\">")
                        .append(calculation(map(body), mode, decimals)).append("</div>");
            }
            default -> {
            }
        }
        return html.toString();
    }

    private static String substituted(Map<String, Object> node, Integer decimals) {
        String type = String.valueOf(node.get("type"));
        return switch (type) {
            case "binary_operator" ->
                    "(" + substituted(map(node.get("lhs")), decimals) + " " + node.get("operator") + " "
                            + (node.get("rhs") == null ? "…" : substituted(map(node.get("rhs")), decimals)) + ")";
            case "unary_prefix_operator" ->
                    node.get("operator") + "(" + substituted(map(node.get("argument")), decimals) + ")";
            case "function_call" -> node.get("name") + "(" + list(node.get("arguments")).stream()
                    .map(item -> valueText(map(item).get("value"), decimals)).reduce((a, b) -> a + ", " + b).orElse("") + ")";
            default -> valueText(node.get("value"), decimals);
        };
    }

    static String valueText(Object value, Integer decimals) {
        Map<String, Object> node = map(value);
        if (node.isEmpty()) return value == null ? "none" : String.valueOf(value);
        return switch (String.valueOf(node.get("type"))) {
            case "none" -> "none";
            case "list" ->
                    "[" + list(node.get("items")).stream().map(item -> valueText(item, decimals)).reduce((a, b) -> a + ", " + b).orElse("") + "]";
            case "map" -> "{" + list(node.get("entries")).stream().map(StructuredDocumentRenderer::map)
                    .map(entry -> valueText(entry.get("key"), null) + ": " + valueText(entry.get("value"), decimals))
                    .reduce((a, b) -> a + ", " + b).orElse("") + "}";
            case "integer", "decimal", "float" -> numberText(node.get("value"), decimals);
            case "callable_ref" -> String.valueOf(node.get("name"));
            default -> String.valueOf(node.get("value"));
        };
    }

    private static String numberText(Object number, Integer decimals) {
        String text = String.valueOf(number);
        if (decimals == null || Set.of("NaN", "Infinity", "-Infinity").contains(text)) return text;
        return new BigDecimal(text).setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object object) {
        return object instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static List<?> list(Object object) {
        return object instanceof List<?> list ? list : List.of();
    }
}
