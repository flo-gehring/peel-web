package de.flogehring.peelserver.impl.run;

import de.flogehring.peel.convenience.RuntimeBuilder;
import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.eval.ExecutionLimits;
import de.flogehring.peel.core.lang.BlockCatalog;
import de.flogehring.peel.run.exceptions.ExecutionLimitException;
import de.flogehring.peel.run.exceptions.PeelException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public final class PeelScriptWorker {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final int MEMORY_LIMIT_EXIT = 2;

    private PeelScriptWorker() {}

    record Input(String script, Map<String, Object> bindings, Limits limits) {}

    record Limits(long timeoutMs, long maxSteps, int maxDepth, long maxTraceUnits, int maxValueSize) {
        ExecutionLimits toExecutionLimits() {
            return new ExecutionLimits(Duration.ofMillis(timeoutMs), maxSteps, maxDepth, maxTraceUnits, maxValueSize);
        }
    }

    record Output(PeelExecutionService.Result result, String errorCode, String message, Map<String, Object> details) {}

    public static void main(String[] args) throws IOException {
        try {
            var input = JSON.readValue(Path.of(args[0]).toFile(), Input.class);
            Output output;
            try {
                var program = PeelExecutionService.parseScript(input.script());
                var bindings = new BindingsMapper().toRequestBindings(input.bindings());
                var trace = RuntimeBuilder.standardLanguage().build().run(program, bindings, input.limits().toExecutionLimits());
                output = new Output(new PeelExecutionService.Result(TraceMapOutput.fromProgram(trace),
                        TraceMapOutput.fromValue(trace.result()), BlockCatalog.analyze(program).names()),
                        null, null, Map.of());
            } catch (ExecutionLimitException exception) {
                output = switch (exception.limit()) {
                    case TIME_LIMIT -> new Output(null, "script_timeout", "PEEL script exceeded its execution time limit.",
                            Map.of("timeoutMs", input.limits().timeoutMs()));
                    case STEP_LIMIT, DEPTH_LIMIT, TRACE_LIMIT, VALUE_LIMIT ->
                            new Output(null, "script_resource_limit", "PEEL script exceeded its resource limit.",
                                    Map.of("limit", exception.limit().name()));
                };
            } catch (PeelException exception) {
                output = new Output(null, "peel_error", exception.getMessage(),
                        Map.of("category", exception.getClass().getSimpleName()));
            } catch (IllegalArgumentException exception) {
                output = new Output(null, "bad_request", exception.getMessage(), Map.of());
            }
            JSON.writeValue(Path.of(args[1]).toFile(), output);
        } catch (OutOfMemoryError | StackOverflowError exception) {
            // Leave no computation or partial trace alive after a resource failure.
            Runtime.getRuntime().halt(MEMORY_LIMIT_EXIT);
        }
    }
}
