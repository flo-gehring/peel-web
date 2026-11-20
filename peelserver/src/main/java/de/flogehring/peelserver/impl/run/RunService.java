package de.flogehring.peelserver.impl.run;

import de.flogehring.peelserver.api.data.scripts.RunRequest;
import de.flogehring.peelserver.api.data.scripts.RunResponse;
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
        ExpectedBindings.require(request.expectedBindings(), request.bindings().keySet());
        var trace = peelExecutionService.execute(request.script(), request.bindings());
        return new RunResponse(trace.trace(), trace.value());
    }
}
