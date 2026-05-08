package de.flogehring.peelserver.run.render;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;

import java.util.Map;

public final class TraceRenderingPebbleExtension extends AbstractExtension {

    private final Filter renderTraceExpressionFilter;

    public TraceRenderingPebbleExtension() {
        this.renderTraceExpressionFilter = new TraceExpressionRenderFilter();
    }

    @Override
    public Map<String, Filter> getFilters() {
        return Map.of("renderTraceExpression", renderTraceExpressionFilter);
    }
}
