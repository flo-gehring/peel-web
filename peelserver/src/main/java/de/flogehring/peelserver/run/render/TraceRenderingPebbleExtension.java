package de.flogehring.peelserver.run.render;

import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.renderconfig.ExpressionRenderTemplate;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TraceRenderingPebbleExtension extends AbstractExtension {

    private final ExpressionRenderConfiguration expressionRenderConfiguration;

    public TraceRenderingPebbleExtension(ExpressionRenderConfiguration configuration) {
        this.expressionRenderConfiguration = configuration;
    }

    @Override
    public Map<String, Filter> getFilters() {
        Map<String, Filter> result = new HashMap<>();
        result.put("renderTraceExpression", new TraceExpressionRenderFilter(expressionRenderConfiguration.getDefaultTemplates(), expressionRenderConfiguration));
        expressionRenderConfiguration.getNamedOverrides()
                .forEach((name, template) -> result.put(name, new TraceExpressionRenderFilter(template, expressionRenderConfiguration)));
        return Collections.unmodifiableMap(result);
    }
}
