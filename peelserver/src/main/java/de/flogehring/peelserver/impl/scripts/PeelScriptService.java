package de.flogehring.peelserver.impl.scripts;

import de.flogehring.peelserver.api.data.scripts.*;
import de.flogehring.peelserver.api.services.PeelScriptController;
import de.flogehring.peelserver.api.error.ResourceNotFoundException;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.run.RunService;
import de.flogehring.peelserver.impl.run.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PeelScriptService implements PeelScriptController {

    private final PeelScriptRepository peelScriptRepository;

    private final RunService runService;
    private final ValidationService validationService;

    @Override
    public RunResponse run(RunRequest request) {
        return runService.run(request);
    }

    @Override
    public ValidationResponse validate(ValidationRequest request) {
        return validationService.validate(request);
    }

    @Override
    public ScriptDtoResponse saveScript(CreateScriptRequest request) {
        String scriptContent = request.script();
        ProjectId projectId = new ProjectId(request.projectIdApi().id());
        PeelScriptPersistence created = PeelScriptPersistence.newScript(
                projectId,request.name(), scriptContent
        );
        PeelScriptPersistence saved = peelScriptRepository.save(created);
        return toResponse(saved);
    }

    @Override
    public ScriptDtoResponse updateScript(String id, ScriptUpdateRequest request) {
        PeelScriptPersistence existing = peelScriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Script not found: " + id));
        PeelScriptPersistence updated = existing.update(
                request.name(),
                requireScript(request.scriptContent())
        );
        PeelScriptPersistence saved = peelScriptRepository.save(updated);
        return toResponse(saved);
    }

    @Override
    public ScriptDtoResponse getScript(String id) {
        PeelScriptPersistence peelScript = peelScriptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Script not found: " + id));
        return toResponse(peelScript);
    }

    @Override
    public void deleteScript(String id) {
        peelScriptRepository.deleteById(id);
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
}
