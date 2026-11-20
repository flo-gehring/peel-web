package de.flogehring.peelserver.impl.run;

import de.flogehring.peel.analysis.AnalysisResult;
import de.flogehring.peel.analysis.PeelAnalysis;
import de.flogehring.peel.analysis.ProvidedSymbol;
import de.flogehring.peel.analysis.SymbolKind;
import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peelserver.api.data.scripts.AnalyzeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
public class AnalysisService {
    private static final int MAX_BINDINGS = 256;
    private static final int MAX_BINDING_TEXT = 64 * 1024;
    private final Semaphore analysisSlots = new Semaphore(2);

    public AnalysisResult analyze(AnalyzeRequest request) {
        PeelExecutionService.requireScriptSize(request.script());
        var bindings = request.expectedBindings();
        if (bindings != null) {
            if (bindings.size() > MAX_BINDINGS) {
                throw new IllegalArgumentException("Analysis can accept at most 256 expected bindings");
            }
            long textLength = 0;
            for (var binding : bindings) {
                if (binding == null || binding.name() == null || binding.description() == null) {
                    throw new IllegalArgumentException("Expected bindings need a valid PEEL identifier and a description");
                }
                textLength += (long) binding.name().length() + binding.description().length();
                if (textLength > MAX_BINDING_TEXT) {
                    throw new IllegalArgumentException("Expected binding names and descriptions can contain at most 65,536 characters in total");
                }
            }
        }
        if (!analysisSlots.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Script analysis is busy. Try again shortly.");
        }
        try {
            ExpectedBindings.validate(bindings);
            return PeelAnalysis.analyze(request.script(), bindings.stream()
                    .map(binding -> new ProvidedSymbol(binding.name(), SymbolKind.VARIABLE, binding.description(), -1)).toList(),
                    RuntimeFactory.defaultSymbols());
        } catch (StackOverflowError error) {
            throw new BindingInputException("analysis_resource_limit", "PEEL script is too deeply nested to analyze.", Map.of());
        } finally {
            analysisSlots.release();
        }
    }
}
