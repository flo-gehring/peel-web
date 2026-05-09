package de.flogehring.peelserver.run.render;

import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;

import java.util.Map;

public final class TraceRenderingPebbleExtension extends AbstractExtension {

    private final Filter renderTraceExpressionFilter;

    public TraceRenderingPebbleExtension() {
        this(ExpressionRenderConfiguration.defaultConfig());
    }

    public TraceRenderingPebbleExtension(ExpressionRenderConfiguration configuration) {
        this.renderTraceExpressionFilter = new TraceExpressionRenderFilter(configuration);
    }

    @Override
    public Map<String, Filter> getFilters() {
        return Map.of("renderTraceExpression", renderTraceExpressionFilter);
    }
}
