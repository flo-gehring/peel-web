package de.flogehring.peelserver.run;

import de.flogehring.peel.convenience.RuntimeFactory;
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

    public TraceProgram execute(String script, Map<String, Object> bindings) {
        Program program = PeelGrammar.parse(script);
        RequestBindings requestBindings = bindingsMapper.toRequestBindings(bindings);
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        return runtime.run(program, requestBindings);
    }
}
