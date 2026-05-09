package de.flogehring.peelserver.renderconfig;

import de.flogehring.peel.core.trace.TraceExpressionKind;
import lombok.Getter;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class ExpressionRenderConfiguration {

    private final Map<TraceExpressionKind, String> templates;

    private ExpressionRenderConfiguration(Map<TraceExpressionKind, String> templates) {
        this.templates = templates;
    }

    public static ExpressionRenderConfiguration of(
            Map<TraceExpressionKind, String> render
    ) {
        return new ExpressionRenderConfiguration(Map.copyOf(new EnumMap<>(render)));
    }

    public static ExpressionRenderConfiguration defaultConfig() {
        Map<TraceExpressionKind, String> templates = new LinkedHashMap<>();
        templates.put(TraceExpressionKind.LITERAL, "{{ valueText }}");
        templates.put(TraceExpressionKind.BINARY_OPERATOR, "{{  lhs | renderTraceExpression }} {{ operator }} {{ rhs | renderTraceExpression }}");
        templates.put(TraceExpressionKind.UNARY_PREFIX_OPERATOR, "{{ operatorSafe }}{{ argumentRendered }}");
        templates.put(TraceExpressionKind.VARIABLE_NAME, "{{ variableNameOrValue }}");
        templates.put(TraceExpressionKind.FUNCTION_CALL, "{{name}}({% for argument in arguments %}{{argument.value.value}}{% if not loop.last %}, {% endif %}{% endfor %})");
        templates.put(TraceExpressionKind.RETURN_EXPR, "return {{ expressionRendered }}");
        templates.put(TraceExpressionKind.ASSIGNMENT, "{{ variableName }} = {{ expression | renderTraceExpression }}");
        templates.put(TraceExpressionKind.IF_STATEMENT, "if ({{ conditionsJoined }}) {{ executedBlockRendered }}");
        templates.put(TraceExpressionKind.WHILE_LOOP, "while (...) { ... } // iterations={{ iterationsCount }}");
        templates.put(
                TraceExpressionKind.FOR_EACH_LOOP,
                "for ({{ loopVariableName }} in {{ iterableRendered }}) { ... } // iterations={{ iterationsCount }}"
        );
        templates.put(TraceExpressionKind.LIST_LITERAL, "[{{ elementsJoined }}]");
        templates.put(TraceExpressionKind.MAP_LITERAL, "{{ mapLiteralRendered }}");
        templates.put(TraceExpressionKind.SELECTOR, "{{ targetRendered }}[{{ selectorRendered }}]");
        templates.put(TraceExpressionKind.BLOCK, "{ {{ contentJoined }} }");
        return new ExpressionRenderConfiguration(Map.copyOf(new EnumMap<>(templates)));
    }

    public ExpressionRenderConfiguration merge(ExpressionRenderConfiguration expressionRenderConfiguration) {
        Map<TraceExpressionKind, String> merged = new EnumMap<>(this.templates);
        merged.putAll(expressionRenderConfiguration.getTemplates());
        return new ExpressionRenderConfiguration(Map.copyOf(merged));
    }
}
