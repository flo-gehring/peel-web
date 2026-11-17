package de.flogehring.peelserver.impl.run;

import de.flogehring.peel.analysis.AnalysisResult;
import de.flogehring.peel.analysis.PeelAnalysis;
import de.flogehring.peel.analysis.ProvidedSymbol;
import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peelserver.api.data.scripts.AnalyzeRequest;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {
    public AnalysisResult analyze(AnalyzeRequest request) {
        ExpectedBindings.validate(request.expectedBindings());
        return PeelAnalysis.analyze(request.script(), request.expectedBindings().stream()
                .map(binding -> new ProvidedSymbol(binding.name(), "variable", binding.description(), -1)).toList(),
                RuntimeFactory.defaultSymbols());
    }
}
