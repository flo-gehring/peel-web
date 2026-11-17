package de.flogehring.peelserver;

import de.flogehring.peelserver.api.data.template.DocumentNode;

import java.util.List;
import java.util.Map;

public final class DocumentFixtures {
    public static DocumentNode doc(DocumentNode... nodes) {
        return new DocumentNode("doc", Map.of(), List.of(nodes), null, List.of());
    }

    public static DocumentNode calculation(String scriptId) {
        return new DocumentNode("peelCalculation", Map.of("scriptId", scriptId, "scope", "script", "mode", "detailed"), List.of(), null, List.of());
    }

    public static DocumentNode textDocument() {
        return doc(new DocumentNode("paragraph", Map.of(), List.of(new DocumentNode("text", Map.of(), List.of(), "Test", List.of())), null, List.of()));
    }

    public static DocumentNode table(String scriptId) {
        var cell = new DocumentNode("peelValue", Map.of("scope", "current"), List.of(), null, List.of());
        var paragraph = new DocumentNode("paragraph", Map.of(), List.of(cell), null, List.of());
        var column = new DocumentNode("peelTableColumn", Map.of("title", "Value", "align", "right"), List.of(paragraph), null, List.of());
        return new DocumentNode("peelTable", Map.of("scope", "script", "scriptId", scriptId), List.of(column), null, List.of());
    }
}
