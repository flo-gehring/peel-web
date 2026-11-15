package de.flogehring.peelserver;

import de.flogehring.peelserver.api.data.template.DocumentNode;
import java.util.*;

public final class DocumentFixtures {
    public static DocumentNode doc(DocumentNode... nodes) { return new DocumentNode("doc", Map.of(), List.of(nodes), null, List.of()); }
    public static DocumentNode calculation(String scriptId) {
        return new DocumentNode("peelCalculation", Map.of("scriptId", scriptId, "scope", "script", "mode", "detailed"), List.of(), null, List.of());
    }
    public static DocumentNode textDocument() {
        return doc(new DocumentNode("paragraph", Map.of(), List.of(new DocumentNode("text", Map.of(), List.of(), "Test", List.of())), null, List.of()));
    }
}
