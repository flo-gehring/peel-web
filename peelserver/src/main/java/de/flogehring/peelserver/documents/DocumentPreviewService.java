package de.flogehring.peelserver.documents;

import de.flogehring.peelserver.api.*;
import de.flogehring.peelserver.run.RunService;
import de.flogehring.peelserver.scripts.PeelScript;
import de.flogehring.peelserver.scripts.PeelScriptRepository;
import de.flogehring.peelserver.scripts.PeelScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentPreviewService {

    private final PeelScriptRepository peelScriptRepository;
    private final PeelScriptService peelExecutionService;
    private final RunService runService;

    public DocumentPreviewResponse preview(DocumentPreviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        Map<String, Object> content = request.content();
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("content must not be empty");
        }

        Map<String, Object> bindings = request.exampleBindings() == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(request.exampleBindings()));

        List<DocumentPreviewReferenceStatus> references = new ArrayList<>();
        List<DocumentPreviewDiagnostic> diagnostics = new ArrayList<>();
        Object rendered = renderNode(content, bindings, references, diagnostics);

        if (!(rendered instanceof Map<?, ?> renderedMap)) {
            throw new IllegalStateException("rendered content must resolve to an object");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> renderedContent = (Map<String, Object>) renderedMap;
        return new DocumentPreviewResponse(
                renderedContent,
                Collections.unmodifiableList(new ArrayList<>(references)),
                Collections.unmodifiableList(new ArrayList<>(diagnostics))
        );
    }

    private Object renderNode(
            Object node,
            Map<String, Object> bindings,
            List<DocumentPreviewReferenceStatus> references,
            List<DocumentPreviewDiagnostic> diagnostics
    ) {
        if (node instanceof Map<?, ?> mapNode) {
            if (isScriptReference(mapNode)) {
                return renderScriptReference(mapNode, bindings, references, diagnostics);
            }

            LinkedHashMap<String, Object> rendered = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapNode.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                rendered.put(key, renderNode(entry.getValue(), bindings, references, diagnostics));
            }
            return Collections.unmodifiableMap(rendered);
        }

        if (node instanceof List<?> listNode) {
            List<Object> renderedList = new ArrayList<>(listNode.size());
            for (Object item : listNode) {
                renderedList.add(renderNode(item, bindings, references, diagnostics));
            }
            return Collections.unmodifiableList(renderedList);
        }

        return node;
    }

    private boolean isScriptReference(Map<?, ?> node) {
        Object type = node.get("type");
        return type instanceof String stringType && stringType.equals("scriptRef");
    }

    private Object renderScriptReference(
            Map<?, ?> scriptRef,
            Map<String, Object> bindings,
            List<DocumentPreviewReferenceStatus> references,
            List<DocumentPreviewDiagnostic> diagnostics
    ) {
        String refId = asString(scriptRef.get("refId"));
        String scriptId = asString(scriptRef.get("scriptId"));

        if (refId == null || refId.isBlank()) {
            refId = "unknown-ref";
        }

        if (scriptId == null || scriptId.isBlank()) {
            references.add(new DocumentPreviewReferenceStatus(refId, "", "error"));
            diagnostics.add(new DocumentPreviewDiagnostic(refId, "invalid_reference", "scriptId is missing"));
            return errorReplacement(refId, "Missing script reference");
        }

        Optional<PeelScript> optionalScript = peelScriptRepository.findById(scriptId);
        if (optionalScript.isEmpty()) {
            references.add(new DocumentPreviewReferenceStatus(refId, scriptId, "error"));
            diagnostics.add(new DocumentPreviewDiagnostic(refId, "script_not_found", "Script not found: " + scriptId));
            return errorReplacement(refId, "Missing script");
        }

        try {
            RunResponse run = runService.run(new RunRequest(
                    optionalScript.get().getScript(),
                    bindings
            ));
            references.add(new DocumentPreviewReferenceStatus(refId, scriptId, "ok"));
            return Map.of(
                    "type", "scriptResult",
                    "refId", refId,
                    "scriptId", scriptId,
                    "value", run.result(),
                    "trace", run.trace()
            );
        } catch (RuntimeException ex) {
            references.add(new DocumentPreviewReferenceStatus(refId, scriptId, "error"));
            diagnostics.add(new DocumentPreviewDiagnostic(refId, "script_runtime_error", ex.getMessage()));
            return errorReplacement(refId, "Script execution failed");
        }
    }

    private String asString(Object value) {
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return null;
    }

    private Map<String, Object> errorReplacement(String refId, String message) {
        return Map.of(
                "type", "scriptError",
                "refId", refId,
                "message", message
        );
    }
}
