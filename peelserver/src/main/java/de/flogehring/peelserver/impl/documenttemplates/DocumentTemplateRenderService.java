package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.parse.PeelGrammar;
import de.flogehring.peelserver.api.data.template.DocumentPreviewResponse;
import de.flogehring.peelserver.impl.run.PeelExecutionService;
import de.flogehring.peelserver.impl.run.render.ExpressionRenderer;
import lombok.RequiredArgsConstructor;
import org.openpdf.pdf.ITextRenderer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DocumentTemplateRenderService {

    private final PeelExecutionService peelExecutionService;

    private record Rendered(String html, List<String> warnings) {
    }

    public String render(Document document, Map<String, Object> bindings) {
        return renderContent(document, bindings).html();
    }

    private Rendered renderContent(Document document, Map<String, Object> bindings) {
        document.document().validateDocument();
        Map<String, TraceProgram> traces = new LinkedHashMap<>();
        Map<String, List<String>> definitions = new LinkedHashMap<>();
        document.scripts().forEach((id, script) -> {
            definitions.put(id, PeelGrammar.parseWithBlocks(script.script()).blockNames());
            traces.put(id, peelExecutionService.execute(script.script(), bindings == null ? Map.of() : bindings));
        });
        var renderer = new StructuredDocumentRenderer(new TraceSelection(traces, definitions),
                ExpressionRenderer.of(document.globalRenderConfiguration()));
        String body = renderer.render(document.document());
        String html = """
                <html xmlns="http://www.w3.org/1999/xhtml"><head><meta charset="UTF-8" />
                <style>
                @page { size: A4; margin: 18mm; }
                body { font-family: sans-serif; font-size: 10pt; color: #202020; }
                p { margin: 0 0 6pt; } h1,h2,h3 { page-break-after: avoid; }
                .peel-equation { font-family: monospace; overflow-wrap: break-word; }
                .peel-calculation { margin: 8pt 0; }
                .peel-repeat { margin: 10pt 0; }
                .peel-table { width: 100%; table-layout: fixed; border-collapse: collapse; margin: 10pt 0; -fs-table-paginate: paginate; }
                .peel-table thead { display: table-header-group; }
                .peel-table tr { page-break-inside: avoid; }
                .peel-table th,.peel-table td { border: 1px solid #b8b8b8; padding: 5pt; vertical-align: top; overflow-wrap: break-word; word-wrap: break-word; }
                .peel-table td p { margin: 0 0 3pt; }
                .peel-function { margin-left: 12pt; border-left: 1pt solid #ddd; padding-left: 6pt; }
                .peel-condition,.peel-iteration { color: #555; }
                pre { white-space: pre-wrap; }
                </style></head><body>
                """ + body + "</body></html>";
        return new Rendered(html, renderer.warnings());
    }

    public DocumentPreviewResponse preview(Document document, Map<String, Object> bindings) {
        var rendered = renderContent(document, bindings);
        return new DocumentPreviewResponse(pdf(rendered.html(), ByteArrayOutputStream::toByteArray), rendered.warnings());
    }

    public <T> T renderPdf(
            Document document,
            Map<String, Object> bindings,
            Function<ByteArrayOutputStream, T> processor
    ) {
        return pdf(renderContent(document, bindings).html(), processor);
    }

    private <T> T pdf(String html, Function<ByteArrayOutputStream, T> processor) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            outputStream.flush();
            return processor.apply(outputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("PDF rendering failed: " + e.getMessage(), e);
        }
    }
}
