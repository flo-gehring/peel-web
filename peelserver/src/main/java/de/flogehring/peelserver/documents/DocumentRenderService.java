package de.flogehring.peelserver.documents;

import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.run.PeelExecutionService;
import de.flogehring.peelserver.run.render.TraceRenderingPebbleExtension;
import io.pebbletemplates.pebble.PebbleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.Map;
import java.util.stream.Collectors;

import static de.flogehring.peelserver.util.StreamUtil.transformMapValues;

@Service
@RequiredArgsConstructor
public class DocumentRenderService {

    private final PeelExecutionService peelExecutionService;

    public String render(Document document, Map<String, Object> bindings) {
        Map<String, TraceProgram> runScripts = transformMapValues(
                document.scriptNameTags(),
                script -> peelExecutionService.execute(script.script(), bindings)
        );
        ExpressionRenderConfiguration expressionRenderConfiguration = document.globalRenderConfiguration().merge(document.localOverrides());
        PebbleEngine engine = new PebbleEngine.Builder()
                .extension(new TraceRenderingPebbleExtension(expressionRenderConfiguration))
                .build();
        Map<String, Object> programmContext = runScripts.entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Map.of(
                                "statements", TraceMapOutput.programmStatements(entry.getValue()),
                                "result", TraceMapOutput.fromValue(entry.getValue().result())
                        )
                )
        );
        StringWriter writer = new StringWriter();
        try {
            engine.getLiteralTemplate(document.template()).evaluate(writer, programmContext);
            return writer.toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Template rendering failed: " + ex.getMessage(), ex);
        }
    }
}
