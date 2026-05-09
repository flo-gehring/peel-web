package de.flogehring.peelserver.run.render;

import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

public final class TraceExpressionRenderFilter implements Filter {

    private final ExpressionRenderer expressionRenderer;

    public TraceExpressionRenderFilter(ExpressionRenderConfiguration configuration) {
        this.expressionRenderer = ExpressionRenderer.of(configuration);
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of();
    }

    @Override
    public Object apply(
            Object input,
            Map<String, Object> args,
            PebbleTemplate self,
            EvaluationContext context,
            int lineNumber
    ) {
        if (!ExpressionRenderer.supportsExpression(input)) {
            return input;
        }
        String rendered = expressionRenderer.renderExpressionNode(input);
        return new SafeString(rendered);
    }
}
