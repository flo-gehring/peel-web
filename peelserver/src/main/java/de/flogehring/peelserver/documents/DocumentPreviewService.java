package de.flogehring.peelserver.documents;

import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peelserver.api.DocumentPreviewRequest;
import de.flogehring.peelserver.api.DocumentPreviewResponse;
import de.flogehring.peelserver.run.PeelExecutionService;
import de.flogehring.peelserver.run.render.TraceTemplateRenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentPreviewService {

    private final PeelExecutionService peelExecutionService;
    private final TraceTemplateRenderService traceTemplateRenderService;

    public DocumentPreviewResponse preview(DocumentPreviewRequest request) {
        if (request.script().isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }

        TraceProgram traceProgram = peelExecutionService.execute(request.script(), request.bindings());
        String html = traceTemplateRenderService.render(
                traceProgram,
                request.template()
        );

        return new DocumentPreviewResponse(
                html,
                TraceMapOutput.fromProgram(traceProgram),
                TraceMapOutput.fromValue(traceProgram.result())
        );
    }
}
