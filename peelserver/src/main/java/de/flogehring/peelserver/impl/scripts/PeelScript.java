package de.flogehring.peelserver.impl.scripts;

public record PeelScript(String description, String script,
                         java.util.List<de.flogehring.peelserver.impl.run.ExpectedBinding> expectedBindings) {
}
