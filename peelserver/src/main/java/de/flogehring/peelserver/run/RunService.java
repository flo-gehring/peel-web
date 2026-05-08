package de.flogehring.peelserver.run;

import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peelserver.api.RunRequest;
import de.flogehring.peelserver.api.RunResponse;
import de.flogehring.peelserver.api.TraceRenderRequest;
import de.flogehring.peelserver.api.TraceRenderResponse;
import de.flogehring.peelserver.run.render.TraceTemplateRenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RunService {

    private final PeelExecutionService peelExecutionService;
    private final TraceTemplateRenderService traceTemplateRenderService;

    public RunResponse run(RunRequest request) {
        if (request == null || request.script().isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        TraceProgram traceProgram = peelExecutionService.execute(request.script(), request.bindings());
        return new RunResponse(
                TraceMapOutput.fromProgram(traceProgram),
                TraceMapOutput.fromValue(traceProgram.result())
        );
    }

    public TraceRenderResponse renderTrace(TraceRenderRequest request) {
        if (request == null || request.script() == null || request.script().isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        TraceProgram traceProgram = peelExecutionService.execute(request.script(), request.bindings());
        return new TraceRenderResponse(
                traceTemplateRenderService.render(traceProgram, request.template()),
                TraceMapOutput.fromProgram(traceProgram),
                TraceMapOutput.fromValue(traceProgram.result())
        );
    }
}
