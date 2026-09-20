package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.impl.run.PeelExecutionService;
import de.flogehring.peelserver.impl.run.render.TraceRenderingPebbleExtension;
import io.pebbletemplates.pebble.PebbleEngine;
import lombok.RequiredArgsConstructor;
import org.openpdf.pdf.ITextRenderer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.flogehring.peelserver.impl.util.StreamUtil.transformMapValues;

@Service
@RequiredArgsConstructor
public class DocumentRenderService {

    private final PeelExecutionService peelExecutionService;

    public String render(Document document, Map<String, Object> bindings) {
        Map<String, TraceProgram> runScripts = transformMapValues(
                document.scriptNameTags(),
                script -> peelExecutionService.execute(script.script(), bindings)
        );
        ExpressionRenderConfiguration expressionRenderConfiguration = document.globalRenderConfiguration();
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
        HashMap<String, Object> context = new HashMap<>(bindings);
        context.putAll(programmContext);
        StringWriter writer = new StringWriter();
        try {
            engine.getLiteralTemplate(document.template()).evaluate(writer, context);
            return writer.toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Template rendering failed: " + ex.getMessage(), ex);
        }
    }

    public <T> T renderPdf(
            Document document,
            Map<String, Object> bindings,
            Function<ByteArrayOutputStream, T> processor
    ) {
        String html = render(document, bindings);
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            outputStream.flush();
            return processor.apply(outputStream);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
