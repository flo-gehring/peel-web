package de.flogehring.peelserver.run;

import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peelserver.api.scripts.RunRequest;
import de.flogehring.peelserver.api.scripts.RunResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RunService {

    private final PeelExecutionService peelExecutionService;

    public RunResponse run(RunRequest request) {
        if (request.script().isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        TraceProgram traceProgram = peelExecutionService.execute(request.script(), request.bindings());
        return new RunResponse(
                TraceMapOutput.fromProgram(traceProgram),
                TraceMapOutput.fromValue(traceProgram.result())
        );
    }
}