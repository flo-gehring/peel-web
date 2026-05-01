package de.flogehring.peelserver.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.convenience.output.TraceOutput;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.eval.RequestBindings;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.parse.PeelGrammar;
import de.flogehring.peelserver.api.RunRequest;
import de.flogehring.peelserver.api.RunResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RunService {

    private final BindingsMapper bindingsMapper;

    public RunResponse run(RunRequest request) {
        if (request == null || request.script() == null || request.script().isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }

        Program program = PeelGrammar.parse(request.script());
        RequestBindings requestBindings = bindingsMapper.toRequestBindings(request.bindings());

        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram traceProgram = runtime.run(program, requestBindings);
        Map<String, Object> trace = TraceOutput.asMap(traceProgram);
        Map<String, Object> result = extractResult(trace);

        return new RunResponse(trace, result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractResult(Map<String, Object> trace) {
        Object result = trace.get("result");
        if (result instanceof Map<?, ?> resultMap) {
            return (Map<String, Object>) resultMap;
        }
        throw new IllegalStateException("Trace output did not contain a map result");
    }
}
