package de.flogehring.peelserver.impl.run.render;

import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderTemplate;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

public final class TraceExpressionRenderFilter implements Filter {

    private final ExpressionRenderer expressionRenderer;
    private final ExpressionRenderer.RenderWork work;

    TraceExpressionRenderFilter(ExpressionRenderTemplate expressionRenderTemplate,
                                ExpressionRenderConfiguration configuration,
                                ExpressionRenderer.RenderWork work) {
        this.expressionRenderer = ExpressionRenderer.of(configuration, expressionRenderTemplate);
        this.work = work;
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
        String rendered = work == null ? expressionRenderer.renderExpressionNode(input)
                : expressionRenderer.renderExpressionNode(input, work);
        return new SafeString(rendered);
    }
}
