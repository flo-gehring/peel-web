package de.flogehring.peelserver.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.convenience.output.TraceOutput;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.eval.RequestBindings;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.parse.PeelGrammar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PeelExecutionService {

    private final BindingsMapper bindingsMapper;

    public ExecutionResult execute(String script, Map<String, Object> bindings) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }

        Program program = PeelGrammar.parse(script);
        RequestBindings requestBindings = bindingsMapper.toRequestBindings(bindings);

        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram traceProgram = runtime.run(program, requestBindings);
        Map<String, Object> trace = TraceOutput.asMap(traceProgram);
        Map<String, Object> result = extractResult(trace);
        return new ExecutionResult(trace, result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractResult(Map<String, Object> trace) {
        Object result = trace.get("result");
        if (result instanceof Map<?, ?> resultMap) {
            return (Map<String, Object>) resultMap;
        }
        throw new IllegalStateException("Trace output did not contain a map result");
    }

    public record ExecutionResult(Map<String, Object> trace, Map<String, Object> result) {
    }
}
