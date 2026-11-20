package de.flogehring.peelserver.impl.run.render;

import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TraceRenderingPebbleExtension extends AbstractExtension {

    private final ExpressionRenderConfiguration expressionRenderConfiguration;
    private final ExpressionRenderer.RenderWork work;

    public TraceRenderingPebbleExtension(ExpressionRenderConfiguration configuration) {
        this(configuration, null);
    }

    TraceRenderingPebbleExtension(ExpressionRenderConfiguration configuration, ExpressionRenderer.RenderWork work) {
        this.expressionRenderConfiguration = configuration;
        this.work = work;
    }

    @Override
    public Map<String, Filter> getFilters() {
        Map<String, Filter> result = new HashMap<>();
        result.put("renderTraceExpression", new TraceExpressionRenderFilter(
                expressionRenderConfiguration.getDefaultTemplates(), expressionRenderConfiguration, work));
        expressionRenderConfiguration.getNamedOverrides()
                .forEach((name, template) -> result.put(name,
                        new TraceExpressionRenderFilter(template, expressionRenderConfiguration, work)));
        return Collections.unmodifiableMap(result);
    }
}
