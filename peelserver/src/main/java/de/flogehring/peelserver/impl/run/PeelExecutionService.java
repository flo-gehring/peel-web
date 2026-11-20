package de.flogehring.peelserver.impl.run;

import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.parse.PeelGrammar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class PeelExecutionService {
    private static final int MAX_SCRIPT_LENGTH = 64 * 1024;
    private static final long MAX_TRACE_BYTES = 16 * 1024 * 1024;
    private final long timeoutMs;
    private final PeelScriptWorker.Limits limits;
    private final Semaphore workers;

    public PeelExecutionService(
            @Value("${peel.execution.timeout:5s}") Duration timeout,
            @Value("${peel.execution.max-workers:2}") int maxWorkers,
            @Value("${peel.execution.max-steps:100000}") long maxSteps,
            @Value("${peel.execution.max-depth:64}") int maxDepth,
            @Value("${peel.execution.max-trace-units:500000}") long maxTraceUnits,
            @Value("${peel.execution.max-value-size:65536}") int maxValueSize
    ) {
        this.timeoutMs = timeout.toMillis();
        if (timeoutMs < 1 || maxWorkers < 1) {
            throw new IllegalArgumentException("Script timeout and worker count must be positive");
        }
        // Leave part of the hard deadline for worker startup, parsing and serialization.
        this.limits = new PeelScriptWorker.Limits(timeoutMs - timeoutMs / 5,
                maxSteps, maxDepth, maxTraceUnits, maxValueSize);
        this.limits.toExecutionLimits();
        this.workers = new Semaphore(maxWorkers);
    }

    public record Result(Map<String, Object> trace, Map<String, Object> value, List<String> blockNames) {
    }

    public Result execute(String script, Map<String, Object> bindings) {
        requireScriptSize(script);
        if (!workers.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Script workers are busy. Try again shortly.");
        }

        try {
            // Files avoid pipe backpressure while the server waits on the worker deadline.
            try (var exchange = new WorkerExchange()) {
                PeelScriptWorker.JSON.writeValue(exchange.input().toFile(), new PeelScriptWorker.Input(script, bindings, limits));
                var subprocess = new ProcessBuilder(command(exchange))
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD);
                subprocess.environment().clear();
                Process worker = subprocess.start();
                try {
                    if (!worker.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                        throw new BindingInputException("script_timeout", "PEEL script exceeded its execution time limit.",
                                Map.of("timeoutMs", timeoutMs));
                    }
                    if (worker.exitValue() == PeelScriptWorker.MEMORY_LIMIT_EXIT
                            || (Files.exists(exchange.output()) && Files.size(exchange.output()) > MAX_TRACE_BYTES)) {
                        throw new BindingInputException("script_resource_limit", "PEEL script exceeded its resource limit.", Map.of());
                    }
                    if (worker.exitValue() != 0) throw new IllegalStateException("PEEL script worker failed");
                    var result = PeelScriptWorker.JSON.readValue(exchange.output().toFile(), PeelScriptWorker.Output.class);
                    if (result.errorCode() != null) {
                        throw new BindingInputException(result.errorCode(), result.message(), result.details());
                    }
                    return result.result();
                } finally {
                    stop(worker);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PEEL script execution was interrupted.");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not communicate with PEEL script worker", exception);
        } finally {
            workers.release();
        }
    }

    private static List<String> command(WorkerExchange exchange) throws IOException {
        var javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var command = new ArrayList<>(List.of(javaExecutable, "-Xmx128m"));
        String testClasspath = System.getProperty("peel.worker.classpath");
        String classpath = testClasspath == null ? System.getProperty("java.class.path") : testClasspath;
        if (testClasspath == null && !classpath.contains(java.io.File.pathSeparator) && classpath.endsWith(".jar")) {
            command.addAll(List.of("-jar", classpath, "--peel-script-worker"));
        } else {
            // A Java argument file also works with long Gradle classpaths on Windows.
            Files.writeString(exchange.arguments(), "-cp\n\"" + classpath.replace("\\", "\\\\").replace("\"", "\\\"")
                    + "\"\n" + PeelScriptWorker.class.getName());
            command.add("@" + exchange.arguments());
        }
        command.add(exchange.input().toString());
        command.add(exchange.output().toString());
        return command;
    }

    private static void stop(Process worker) {
        if (!worker.isAlive()) return;
        worker.destroyForcibly();
        // Keep the worker slot until the OS confirms exit.
        worker.onExit().join();
    }

    private record WorkerExchange(Path directory) implements AutoCloseable {
        WorkerExchange() throws IOException {
            this(Files.createTempDirectory("peel-script-"));
        }

        Path input() {
            return directory.resolve("input.json");
        }

        Path output() {
            return directory.resolve("output.json");
        }

        Path arguments() {
            return directory.resolve("java.args");
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(input());
            Files.deleteIfExists(output());
            Files.deleteIfExists(arguments());
            Files.deleteIfExists(directory);
        }
    }

    public static Program parseScript(String script) {
        requireScriptSize(script);
        return PeelGrammar.parse(script);
    }

    static void requireScriptSize(String script) {
        if (script.length() > MAX_SCRIPT_LENGTH) {
            throw new IllegalArgumentException("PEEL scripts can contain at most 65,536 characters");
        }
    }
}
