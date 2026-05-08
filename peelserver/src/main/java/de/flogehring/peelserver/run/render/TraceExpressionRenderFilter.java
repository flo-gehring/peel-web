package de.flogehring.peelserver.run.render;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

public final class TraceExpressionRenderFilter implements Filter {

    @Override
    public List<String> getArgumentNames() {
        return List.of("showValue");
    }

    @Override
    public Object apply(
            Object input,
            Map<String, Object> args,
            PebbleTemplate self,
            EvaluationContext context,
            int lineNumber
    ) {
        if (!DefaultTraceExpressionRenderers.supports(input)) {
            return input;
        }
        boolean showValue = true;
        Object showValueArg = args.get("showValue");
        if (showValueArg instanceof Boolean value) {
            showValue = value;
        } else if (showValueArg != null) {
            showValue = Boolean.parseBoolean(String.valueOf(showValueArg));
        }
        String rendered = DefaultTraceExpressionRenderers.render(input, showValue);
        return new SafeString(rendered);
    }
}
