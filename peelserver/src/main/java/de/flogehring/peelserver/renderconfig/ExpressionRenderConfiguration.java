package de.flogehring.peelserver.renderconfig;

import de.flogehring.peel.core.trace.TraceExpressionKind;
import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class ExpressionRenderConfiguration {

    private final ExpressionRenderTemplate defaultTemplates;
    private final Map<String, ExpressionRenderTemplate> namedOverrides;

    private ExpressionRenderConfiguration(
            ExpressionRenderTemplate defaultTemplates,
            Map<String, ExpressionRenderTemplate> namedOverrides

    ) {
        this.defaultTemplates = defaultTemplates;
        this.namedOverrides = namedOverrides;
    }

    public static ExpressionRenderConfiguration of(
            Map<TraceExpressionKind, String> render,
            Map<String, Map<TraceExpressionKind, String>> namedOverrides
    ) {
        return new ExpressionRenderConfiguration(
                new ExpressionRenderTemplate(Map.copyOf(new EnumMap<>(render))),
                namedOverrides.entrySet().stream().collect(
                        LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), new ExpressionRenderTemplate(Map.copyOf(new EnumMap<>(entry.getValue())))),
                        LinkedHashMap::putAll
                )
        );
    }

    public static ExpressionRenderConfiguration defaultConfig() {
        Map<TraceExpressionKind, String> templates = new EnumMap<>(TraceExpressionKind.class);
        templates.put(TraceExpressionKind.FUNCTION_CALL, "{{name}}({% for argument in arguments %}{{argument.value.value}}{% if not loop.last %}, {% endif %}{% endfor %})");
        templates.put(TraceExpressionKind.BINARY_OPERATOR, "{{  lhs | renderTraceExpression }} {{ operator }} {{ rhs | renderTraceExpression }}");
        templates.put(TraceExpressionKind.LITERAL, "{{ valueText }}");
        templates.put(TraceExpressionKind.UNARY_PREFIX_OPERATOR, "{{ operator }}{{ argument | renderTraceExpression }}");
        templates.put(TraceExpressionKind.VARIABLE_NAME, "{{valueText}}");
        templates.put(TraceExpressionKind.RETURN_EXPR, "return {{ expression | renderTraceExpression }}");
        templates.put(TraceExpressionKind.ASSIGNMENT, "{{ variableName }} = {{ expression | renderTraceExpression }}");
        templates.put(
                TraceExpressionKind.IF_STATEMENT,
                "if ({% for condition in conditions %}{{ condition | renderTraceExpression }}{% if not loop.last %} || {% endif %}{% endfor %}){% if executedBlock != null %} {{ executedBlock | renderTraceExpression }}{% endif %}"
        );
        templates.put(
                TraceExpressionKind.WHILE_LOOP,
                "while ({% if iterations|length > 0 and iterations[0].condition != null %}{{ iterations[0].condition | renderTraceExpression }}{% else %}...{% endif %}) {% if iterations|length > 0 and iterations[0].body != null %}{{ iterations[0].body | renderTraceExpression }}{% else %}{{ '{' }}{{ '}' }}{% endif %}"
        );
        templates.put(
                TraceExpressionKind.FOR_EACH_LOOP,
                "for ({{ variableName }} in {{ iterableExpression | renderTraceExpression }}) {% if iterations|length > 0 and iterations[0].body != null %}{{ iterations[0].body | renderTraceExpression }}{% else %}{{ '{' }}{{ '}' }}{% endif %}"
        );
        templates.put(
                TraceExpressionKind.LIST_LITERAL,
                "[{% for element in elements %}{{ element | renderTraceExpression }}{% if not loop.last %}, {% endif %}{% endfor %}]"
        );
        templates.put(
                TraceExpressionKind.MAP_LITERAL,
                "{{ '{' }}{% for entry in entries %}{{ entry.key | renderTraceExpression }}: {{ entry.value | renderTraceExpression }}{% if not loop.last %}, {% endif %}{% endfor %}{{ '}' }}"
        );
        templates.put(TraceExpressionKind.SELECTOR, "{{ target | renderTraceExpression }}[{{ selector | renderTraceExpression }}]");
        templates.put(
                TraceExpressionKind.BLOCK,
                "{{ '{' }}{% for statement in content %}{{ statement | renderTraceExpression }}{% if not loop.last %}; {% endif %}{% endfor %}{{ '}' }}"
        );
        return new ExpressionRenderConfiguration(new ExpressionRenderTemplate(Collections.unmodifiableMap(templates)), Map.of());
    }
}
