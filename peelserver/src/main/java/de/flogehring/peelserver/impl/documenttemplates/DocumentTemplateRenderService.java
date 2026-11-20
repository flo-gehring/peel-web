package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.api.data.template.DocumentPreviewResponse;
import de.flogehring.peelserver.impl.run.PeelExecutionService;
import de.flogehring.peelserver.impl.run.render.ExpressionRenderer;
import lombok.RequiredArgsConstructor;
import org.openpdf.resource.ImageResource;
import org.openpdf.pdf.ITextRenderer;
import org.openpdf.pdf.ITextOutputDevice;
import org.openpdf.pdf.ITextUserAgent;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class DocumentTemplateRenderService {
    private static final int MAX_PDF_BYTES = 10 * 1024 * 1024;
    private static final int MAX_LINKED_SCRIPTS = 8;

    private final PeelExecutionService peelExecutionService;
    private final Semaphore renderSlots = new Semaphore(2);

    private record Rendered(String html, List<String> warnings) {
    }

    public String render(Document document, Map<String, Object> bindings) {
        return withRenderSlot(() -> renderContent(document, bindings).html());
    }

    private Rendered renderContent(Document document, Map<String, Object> bindings) {
        if (document.scripts().size() > MAX_LINKED_SCRIPTS) {
            throw new IllegalArgumentException("Rendering supports at most 8 linked scripts per document");
        }
        document.document().validateDocument();
        var suppliedBindings = bindings == null ? Map.<String, Object>of() : bindings;
        de.flogehring.peelserver.impl.run.ExpectedBindings.requireDocument(document, suppliedBindings.keySet(), false);
        Map<String, Map<String, Object>> traces = new LinkedHashMap<>();
        Map<String, List<String>> definitions = new LinkedHashMap<>();
        document.scripts().forEach((id, script) -> {
            var result = peelExecutionService.execute(script.script(), suppliedBindings);
            definitions.put(id, result.blockNames());
            traces.put(id, result.trace());
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
        return withRenderSlot(() -> {
            var rendered = renderContent(document, bindings);
            return new DocumentPreviewResponse(pdf(rendered.html(), ByteArrayOutputStream::toByteArray), rendered.warnings());
        });
    }

    public <T> T renderPdf(
            Document document,
            Map<String, Object> bindings,
            Function<ByteArrayOutputStream, T> processor
    ) {
        return withRenderSlot(() -> pdf(renderContent(document, bindings).html(), processor));
    }

    private <T> T withRenderSlot(Supplier<T> render) {
        if (!renderSlots.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Document rendering is busy. Try again shortly.");
        }
        try {
            return render.get();
        } catch (StackOverflowError error) {
            throw new IllegalArgumentException("Document rendering exceeded its nesting limit", error);
        } finally {
            renderSlots.release();
        }
    }

    private <T> T pdf(String html, Function<ByteArrayOutputStream, T> processor) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream() {
            @Override
            public void write(int value) {
                checkOutputSize(1);
                super.write(value);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) {
                checkOutputSize(length);
                super.write(bytes, offset, length);
            }

            private void checkOutputSize(int additionalBytes) {
                if (additionalBytes > MAX_PDF_BYTES - size()) {
                    throw new IllegalArgumentException("A generated PDF can be at most 10 MiB");
                }
            }
        }) {
            var outputDevice = new ITextOutputDevice(ITextRenderer.DEFAULT_DOTS_PER_POINT);
            var defaultStylesheet = ITextRenderer.class.getResource("/resources/css/XhtmlNamespaceHandler.css");
            var userAgent = new ITextUserAgent(outputDevice, ITextRenderer.DEFAULT_DOTS_PER_PIXEL) {
                @Override
                public ImageResource getImageResource(String uri) {
                    // Embedded images bypass the stream allowlist in OpenPDF.
                    return new ImageResource(uri, null);
                }

                @Override
                protected InputStream resolveAndOpenStream(String uri) {
                    // The bundled stylesheet supplies the default layout of paragraphs and tables.
                    if (defaultStylesheet != null && defaultStylesheet.toExternalForm().equals(uri)) {
                        try {
                            return defaultStylesheet.openStream();
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    }
                    return null;
                }
            };
            var renderer = new ITextRenderer(outputDevice, userAgent);
            renderer.setDocumentFromString(html);
            renderer.layout();
            if (renderer.getRootBox().getLayer().getPages().size() > 100) {
                throw new IllegalArgumentException("A generated PDF can contain at most 100 pages");
            }
            renderer.createPDF(outputStream);
            outputStream.flush();
            return processor.apply(outputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("PDF rendering failed: " + e.getMessage(), e);
        }
    }
}
