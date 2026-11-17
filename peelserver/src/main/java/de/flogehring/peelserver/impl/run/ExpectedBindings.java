package de.flogehring.peelserver.impl.run;

import de.flogehring.peel.analysis.PeelAnalysis;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public final class ExpectedBindings {
    private ExpectedBindings() {}

    public static void validate(List<ExpectedBinding> expected) {
        if (expected == null) throw new IllegalArgumentException("expectedBindings must be a list");
        var names = new HashSet<String>();
        for (var binding : expected) {
            if (binding == null || !PeelAnalysis.isIdentifier(binding.name()) || binding.description() == null)
                throw new IllegalArgumentException("Expected bindings need a valid PEEL identifier and a description");
            if (!names.add(binding.name()))
                throw new IllegalArgumentException("Duplicate expected binding: " + binding.name());
        }
    }

    public static void require(List<ExpectedBinding> expected, Collection<String> available) {
        validate(expected);
        var missing = expected.stream().map(ExpectedBinding::name).filter(name -> !available.contains(name)).toList();
        if (!missing.isEmpty()) throw new BindingInputException("missing_bindings",
                "Missing required bindings: " + String.join(", ", missing), java.util.Map.of("missingBindings", missing));
    }

    public static void requireDocument(de.flogehring.peelserver.impl.documenttemplates.Document document,
                                       Collection<String> available, boolean csv) {
        var missingByScript = new java.util.TreeMap<String, List<String>>();
        document.scripts().forEach((id, script) -> {
            validate(script.expectedBindings());
            var missing = script.expectedBindings().stream().map(ExpectedBinding::name)
                    .filter(name -> !available.contains(name)).sorted().toList();
            if (!missing.isEmpty()) missingByScript.put(script.name().isBlank() ? id : script.name(), missing);
        });
        if (!missingByScript.isEmpty()) {
            var missing = missingByScript.values().stream().flatMap(List::stream).distinct().sorted().toList();
            throw new BindingInputException(csv ? "missing_csv_columns" : "missing_bindings",
                    (csv ? "Missing required CSV columns: " : "Missing required bindings: ") + String.join(", ", missing),
                    java.util.Map.of(csv ? "missingColumns" : "missingBindings", missing, "scripts", missingByScript));
        }
    }
}
