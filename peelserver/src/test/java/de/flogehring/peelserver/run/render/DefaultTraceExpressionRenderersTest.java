package de.flogehring.peelserver.run.render;

import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTraceExpressionRenderersTest {

    @Test
    void supportsExpressionMapByType() {
        Map<String, Object> expression = TraceMapOutput.fromExpression(new TraceExpression.Literal(TraceValue.integer(1)));

        assertThat(DefaultTraceExpressionRenderers.supports(expression)).isTrue();
        assertThat(DefaultTraceExpressionRenderers.supports(Map.of("type", "unknown"))).isFalse();
    }

    @Test
    void rendersAssignmentAndValue() {
        TraceExpression.Assignment assignment = new TraceExpression.Assignment(
                "x",
                new TraceExpression.BinaryOperator(
                        "+",
                        TraceValue.integer(3),
                        new TraceExpression.Literal(TraceValue.integer(1)),
                        new TraceExpression.Literal(TraceValue.integer(2)),
                        false
                ),
                TraceValue.integer(3)
        );

        Map<String, Object> expression = TraceMapOutput.fromExpression(assignment);
        String rendered = DefaultTraceExpressionRenderers.render(expression, true);

        assertThat(rendered).contains("x = 1 => 1 + 2 => 2 => 3");
    }

    @Test
    void rendersFunctionCallArguments() {
        TraceExpression.FunctionCall functionCall = new TraceExpression.FunctionCall(
                "add",
                TraceValue.integer(3),
                List.of(
                        new TraceExpression.Literal(TraceValue.integer(1)),
                        new TraceExpression.Literal(TraceValue.integer(2))
                ),
                java.util.Optional.empty(),
                TraceExpression.CalleeSource.variable("add"),
                new TraceExpression.ResolvedCallable(de.flogehring.peel.core.trace.CallableKind.PEEL_FUNCTION, "add", 2)
        );

        Map<String, Object> expression = TraceMapOutput.fromExpression(functionCall);
        String rendered = DefaultTraceExpressionRenderers.render(expression, false);

        assertThat(rendered).isEqualTo("add(1, 2)");
    }
}
