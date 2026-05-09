package de.flogehring.peelserver.scripts;

import de.flogehring.peelserver.PeelScriptController;
import de.flogehring.peelserver.api.scripts.ScriptDtoResponse;
import de.flogehring.peelserver.api.scripts.ScriptSaveRequest;
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
    public ScriptDtoResponse saveScript(ScriptSaveRequest request) {
        String scriptContent = requireScript(request.script());
        if (request.id() != null && !request.id().isBlank()) {
            PeelScriptPersistence existing = peelScriptRepository.findById(request.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Script not found: " + request.id()));
            PeelScriptPersistence updated = existing.update(request.name(), scriptContent);
            PeelScriptPersistence saved = peelScriptRepository.save(updated);
            return toResponse(saved);
        }
        PeelScriptPersistence created = PeelScriptPersistence.newScript(request.name(), scriptContent);
        PeelScriptPersistence saved = peelScriptRepository.save(created);
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
    public ScriptDtoResponse getScript(String id) {
        PeelScriptPersistence peelScript = peelScriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Script not found: " + id));
        return toResponse(peelScript);
    }

    private String requireScript(String script) {
        if (script.isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        return script;
    }

    private ScriptDtoResponse toResponse(PeelScriptPersistence peelScript) {
        return new ScriptDtoResponse(
                peelScript.getId(),
                peelScript.getPeelScript().description(),
                peelScript.getPeelScript().script()
        );
    }

    private ScriptSummaryResponse toSummary(
            PeelScriptPersistence peelScript
    ) {
        return new ScriptSummaryResponse(peelScript.getId(), peelScript.getPeelScript().description());
    }
}
