package de.flogehring.peelserver.run.render;

import io.pebbletemplates.pebble.PebbleEngine;
import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.trace.TraceProgram;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TraceTemplateRenderService {

    private static final String DEFAULT_TEMPLATE = """
            <ul>
            {% for statement in statements %}
              <li>{{ statement | renderTraceExpression }}</li>
            {% endfor %}
            </ul>
            """;

    public String render(TraceProgram traceProgram, String template) {
        String actualTemplate = template == null || template.isBlank() ? DEFAULT_TEMPLATE : template;
        PebbleEngine engine = new PebbleEngine.Builder()
                .extension(new TraceRenderingPebbleExtension())
                .build();

        List<Map<String, Object>> statements = TraceMapOutput.programmStatements(traceProgram);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("statements", statements);
        context.put("program", TraceMapOutput.fromProgram(traceProgram));
        context.put("result", TraceMapOutput.fromValue(traceProgram.result()));

        StringWriter writer = new StringWriter();
        try {
            engine.getLiteralTemplate(actualTemplate).evaluate(writer, context);
            return writer.toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Template rendering failed: " + ex.getMessage(), ex);
        }
    }
}
