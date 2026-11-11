package de.flogehring.peelserver.impl.scripts;

import de.flogehring.peelserver.api.data.scripts.*;
import de.flogehring.peelserver.api.services.PeelScriptController;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.project.ProjectAccessService;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import de.flogehring.peelserver.impl.run.RunService;
import de.flogehring.peelserver.impl.run.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PeelScriptService implements PeelScriptController {

    private final PeelScriptRepository peelScriptRepository;
    private final ProjectAccessService projectAccess;

    private final RunService runService;
    private final ValidationService validationService;

    @Override
    public RunResponse run(RunRequest request, SpringUserDetails user) {
        return projectAccess.active(new ProjectId(request.projectId().id()), user.getUserId(), () -> runService.run(request));
    }

    @Override
    public ValidationResponse validate(ValidationRequest request) {
        return validationService.validate(request);
    }

    @Override
    public ScriptDtoResponse saveScript(CreateScriptRequest request, SpringUserDetails user) {
        return projectAccess.active(new ProjectId(request.projectIdApi().id()), user.getUserId(), () -> {
            String scriptContent = request.script();
            ProjectId projectId = new ProjectId(request.projectIdApi().id());
            projectAccess.requireProjectAccess(projectId, user.getUserId());
            PeelScriptPersistence created = PeelScriptPersistence.newScript(
                    projectId,request.name(), scriptContent
            );
            PeelScriptPersistence saved = peelScriptRepository.save(created);
            return toResponse(saved);
        });
    }

    @Override
    public ScriptDtoResponse updateScript(String id, ScriptUpdateRequest request, SpringUserDetails user) {
        return projectAccess.active(requireScriptAccess(id, user).getProjectId(), user.getUserId(), () -> {
            PeelScriptPersistence existing = requireScriptAccess(id, user);
            PeelScriptPersistence updated = existing.update(
                    request.name(),
                    requireScript(request.scriptContent())
            );
            PeelScriptPersistence saved = peelScriptRepository.save(updated);
            return toResponse(saved);
        });
    }

    @Override
    public ScriptDtoResponse getScript(String id, SpringUserDetails user) {
        PeelScriptPersistence peelScript = requireScriptAccess(id, user);
        return toResponse(peelScript);
    }

    @Override
    public void deleteScript(String id, SpringUserDetails user) {
        projectAccess.active(requireScriptAccess(id, user).getProjectId(), user.getUserId(), () -> {
            requireScriptAccess(id, user);
            peelScriptRepository.deleteById(id);
            return null;
        });
    }

    private PeelScriptPersistence requireScriptAccess(String id, SpringUserDetails user) {
        var script = peelScriptRepository.findById(id).orElseThrow(ProjectAccessService::notFound);
        projectAccess.requireProjectAccess(script.getProjectId(), user.getUserId());
        return script;
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
