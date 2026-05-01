package de.flogehring.peelserver.scripts;

import de.flogehring.peelserver.PeelScriptController;
import de.flogehring.peelserver.api.ScriptResponse;
import de.flogehring.peelserver.api.ScriptSaveRequest;
import de.flogehring.peelserver.api.ScriptSummaryResponse;
import de.flogehring.peelserver.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PeelScriptService implements PeelScriptController {

    private final PeelScriptRepository peelScriptRepository;

    @Override
    public ScriptResponse saveScript(ScriptSaveRequest request) {
        String scriptContent = requireScript(request.script());
        if (request.id() != null && !request.id().isBlank()) {
            PeelScript existing = peelScriptRepository.findById(request.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Script not found: " + request.id()));
            PeelScript updated = existing.update(request.name(), scriptContent);
            PeelScript saved = peelScriptRepository.save(updated);
            return toResponse(saved);
        }
        PeelScript created = PeelScript.newScript(request.name(), scriptContent);
        PeelScript saved = peelScriptRepository.save(created);
        return toResponse(saved);
    }

    @Override
    public List<ScriptSummaryResponse> listScripts() {
        return peelScriptRepository.findAll().stream()
                .map(this::toSummary)
                .sorted(Comparator.comparing(ScriptSummaryResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public ScriptResponse getScript(String id) {
        PeelScript peelScript = peelScriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Script not found: " + id));
        return toResponse(peelScript);
    }

    private String requireScript(String script) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        return script;
    }

    private ScriptResponse toResponse(PeelScript peelScript) {
        return new ScriptResponse(
                peelScript.getId(),
                displayName(peelScript),
                peelScript.getScript()
        );
    }

    private ScriptSummaryResponse toSummary(PeelScript peelScript) {
        return new ScriptSummaryResponse(peelScript.getId(), displayName(peelScript));
    }

    private String displayName(PeelScript peelScript) {
        if (peelScript.getName() == null || peelScript.getName().isBlank()) {
            return "Untitled script";
        }
        return peelScript.getName();
    }
}
