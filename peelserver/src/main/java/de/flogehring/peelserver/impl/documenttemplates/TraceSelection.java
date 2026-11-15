package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.trace.TraceProgram;
import java.util.*;

final class TraceSelection {
    record Occurrence(String scriptId, String path, Map<String, Object> node, Map<String, Object> variablesAtExit) {}
    private final Map<String, List<Occurrence>> byScript = new LinkedHashMap<>();
    private final Map<String, List<String>> definitions;

    TraceSelection(Map<String, TraceProgram> traces, Map<String, List<String>> definitions) {
        this.definitions = definitions;
        traces.forEach((id, trace) -> {
            List<Occurrence> occurrences = new ArrayList<>();
            var root = new LinkedHashMap<String, Object>();
            root.put("type", "block");
            var mapped = TraceMapOutput.fromProgram(trace);
            root.put("content", mapped.get("expressions"));
            root.put("value", mapped.get("result"));
            root.put("variablesAtExit", mapped.get("variablesAtExit"));
            walk(id, root, "/", occurrences);
            byScript.put(id, List.copyOf(occurrences));
        });
    }

    List<Occurrence> select(String scriptId, String block, Occurrence current, boolean relative) {
        if (relative && current == null) throw new IllegalArgumentException("A current trace occurrence is required");
        String id = relative ? current.scriptId() : scriptId;
        var occurrences = byScript.get(id);
        if (occurrences == null) throw new IllegalArgumentException("Unknown script reference: " + id);
        if (block.isBlank()) return List.of(relative ? current : occurrences.getFirst());
        if (!definitions.getOrDefault(id, List.of()).contains(block))
            throw new IllegalArgumentException("Unknown block '" + block + "' in script " + id);
        return occurrences.stream().filter(item -> block.equals(item.node().get("name")))
                .filter(item -> !relative || item.path().startsWith(current.path())).toList();
    }

    @SuppressWarnings("unchecked")
    private static void walk(String id, Object object, String path, List<Occurrence> result) {
        if (object instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) walk(id, list.get(i), path + i + "/", result);
        } else if (object instanceof Map<?, ?> raw) {
            Map<String, Object> node = (Map<String, Object>) raw;
            if ("block".equals(node.get("type")))
                result.add(new Occurrence(id, path, node, (Map<String,Object>) node.get("variablesAtExit")));
            node.forEach((key, value) -> {
                if (!Set.of("value", "result", "binding", "parameterBindings", "resolvedCallable", "variablesAtExit").contains(key))
                    walk(id, value, path + key + "/", result);
            });
        }
    }
}
