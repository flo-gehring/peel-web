package de.flogehring.peelserver.documents;

import de.flogehring.peelserver.api.DocumentPreviewDiagnostic;
import de.flogehring.peelserver.api.DocumentPreviewReferenceStatus;
import de.flogehring.peelserver.api.DocumentPreviewRequest;
import de.flogehring.peelserver.api.DocumentPreviewResponse;
import de.flogehring.peelserver.api.RunRequest;
import de.flogehring.peelserver.api.RunResponse;
import de.flogehring.peelserver.run.RunService;
import de.flogehring.peelserver.scripts.PeelScript;
import de.flogehring.peelserver.scripts.PeelScriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DocumentPreviewService {

    private static final Pattern SCRIPT_TOKEN_PATTERN = Pattern.compile("\\{\\{script:([A-Za-z0-9-]+)}}");

    private final PeelScriptRepository peelScriptRepository;
    private final RunService runService;
    private final ScriptTraceLineFormatter scriptTraceLineFormatter;

    public DocumentPreviewResponse preview(DocumentPreviewRequest request) {
        Map<String, Object> content = request.content();
        Map<String, Object> bindings = request.exampleBindings();
        List<DocumentPreviewReferenceStatus> references = new ArrayList<>();
        List<DocumentPreviewDiagnostic> diagnostics = new ArrayList<>();
        Map<String, ScriptRenderOutcome> evaluationCache = new HashMap<>();
        AtomicInteger tokenCounter = new AtomicInteger();
        Object rendered = renderNode(
                content,
                bindings,
                references,
                diagnostics,
                evaluationCache,
                tokenCounter
        );

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
            List<DocumentPreviewDiagnostic> diagnostics,
            Map<String, ScriptRenderOutcome> evaluationCache,
            AtomicInteger tokenCounter
    ) {
        if (node instanceof Map<?, ?> mapNode) {
            if (isTextNode(mapNode)) {
                return renderTextNode(
                        mapNode,
                        bindings,
                        references,
                        diagnostics,
                        evaluationCache,
                        tokenCounter
                );
            }
            if (isScriptReference(mapNode)) {
                return renderScriptReference(
                        mapNode,
                        bindings,
                        references,
                        diagnostics,
                        evaluationCache
                );
            }
            LinkedHashMap<String, Object> rendered = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapNode.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                rendered.put(
                        key,
                        renderNode(
                                entry.getValue(),
                                bindings,
                                references,
                                diagnostics,
                                evaluationCache,
                                tokenCounter
                        )
                );
            }
            return Collections.unmodifiableMap(rendered);
        }

        if (node instanceof List<?> listNode) {
            List<Object> renderedList = new ArrayList<>(listNode.size());
            for (Object item : listNode) {
                renderedList.add(
                        renderNode(
                                item,
                                bindings,
                                references,
                                diagnostics,
                                evaluationCache,
                                tokenCounter
                        )
                );
            }
            return Collections.unmodifiableList(renderedList);
        }
        return node;
    }

    private boolean isScriptReference(Map<?, ?> node) {
        Object type = node.get("type");
        return type instanceof String stringType && stringType.equals("scriptRef");
    }

    private boolean isTextNode(Map<?, ?> node) {
        return "text".equals(node.get("type")) && node.get("text") instanceof String;
    }

    private Map<String, Object> renderTextNode(
            Map<?, ?> textNode,
            Map<String, Object> bindings,
            List<DocumentPreviewReferenceStatus> references,
            List<DocumentPreviewDiagnostic> diagnostics,
            Map<String, ScriptRenderOutcome> evaluationCache,
            AtomicInteger tokenCounter
    ) {
        String text = asString(textNode.get("text"));
        if (text.isEmpty()) {
            return mapCopy(textNode);
        }
        Matcher matcher = SCRIPT_TOKEN_PATTERN.matcher(text);
        if (!matcher.find()) {
            return mapCopy(textNode);
        }
        StringBuffer replaced = new StringBuffer();
        do {
            String scriptId = matcher.group(1);
            String refId = "script-token-" + tokenCounter.incrementAndGet();
            ScriptRenderOutcome outcome = evaluationCache.computeIfAbsent(
                    scriptId,
                    id -> evaluateScript(id, bindings)
            );
            references.add(new DocumentPreviewReferenceStatus(refId, scriptId, outcome.status()));
            if (outcome.isError()) {
                diagnostics.add(new DocumentPreviewDiagnostic(refId, outcome.code(), outcome.message()));
            }

            matcher.appendReplacement(replaced, Matcher.quoteReplacement(outcome.renderedText()));
        } while (matcher.find());

        matcher.appendTail(replaced);
        LinkedHashMap<String, Object> next = mutableMapCopy(textNode);
        next.put("text", replaced.toString());
        return Collections.unmodifiableMap(next);
    }

    private Object renderScriptReference(
            Map<?, ?> scriptRef,
            Map<String, Object> bindings,
            List<DocumentPreviewReferenceStatus> references,
            List<DocumentPreviewDiagnostic> diagnostics,
            Map<String, ScriptRenderOutcome> evaluationCache
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

        ScriptRenderOutcome outcome = evaluationCache.computeIfAbsent(scriptId, id -> evaluateScript(id, bindings));
        references.add(new DocumentPreviewReferenceStatus(refId, scriptId, outcome.status()));
        if (outcome.isError()) {
            diagnostics.add(new DocumentPreviewDiagnostic(refId, outcome.code(), outcome.message()));
            return errorReplacement(refId, outcome.message());
        }

        return Map.of(
                "type", "scriptResult",
                "refId", refId,
                "scriptId", scriptId,
                "text", outcome.renderedText()
        );
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

    private ScriptRenderOutcome evaluateScript(String scriptId, Map<String, Object> bindings) {
        Optional<PeelScript> optionalScript = peelScriptRepository.findById(scriptId);
        if (optionalScript.isEmpty()) {
            String message = "Script not found: " + scriptId;
            return ScriptRenderOutcome.error("script_not_found", message);
        }

        try {
//            RunResponse run = runService.run(new RunRequest(optionalScript.get().getScript(), bindings));
//            // TODO it is stupid that the scriptTraceLineFormatter  does not use the rich output of the TraceProgramm and an untyped map instead
//            // List<String> lines = scriptTraceLineFormatter.formatProgramLines(run.trace());
//            String renderedText = String.join("\n", lines);
//            if (renderedText.isBlank()) {
//                renderedText = "(empty output)";
//            }
            return ScriptRenderOutcome.ok("rendered");
        } catch (RuntimeException ex) {
            return ScriptRenderOutcome.error("script_runtime_error", ex.getMessage());
        }
    }

    private Map<String, Object> mapCopy(Map<?, ?> source) {
        return Collections.unmodifiableMap(mutableMapCopy(source));
    }

    private LinkedHashMap<String, Object> mutableMapCopy(Map<?, ?> source) {
        LinkedHashMap<String, Object> copied = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() instanceof String key) {
                copied.put(key, entry.getValue());
            }
        }
        return copied;
    }

    private record ScriptRenderOutcome(String status, String renderedText, String code, String message) {

        static ScriptRenderOutcome ok(String renderedText) {
            return new ScriptRenderOutcome("ok", renderedText, "", "");
        }

        static ScriptRenderOutcome error(String code, String message) {
            return new ScriptRenderOutcome(
                    "error",
                    "[script error: " + message + "]",
                    code,
                    message
            );
        }

        boolean isError() {
            return "error".equals(status);
        }
    }
}
