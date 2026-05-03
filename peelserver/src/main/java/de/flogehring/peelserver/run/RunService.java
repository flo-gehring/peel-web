package de.flogehring.peelserver.run;

import de.flogehring.peelserver.api.RunRequest;
import de.flogehring.peelserver.api.RunResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RunService {

    private final PeelExecutionService peelExecutionService;

    public RunResponse run(RunRequest request) {
        if (request == null || request.script() == null || request.script().isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        PeelExecutionService.ExecutionResult executionResult = peelExecutionService.execute(request.script(), request.bindings());
        return new RunResponse(executionResult.trace(), executionResult.result());
    }
}
