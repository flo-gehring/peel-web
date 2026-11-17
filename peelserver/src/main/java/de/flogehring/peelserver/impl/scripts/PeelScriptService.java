package de.flogehring.peelserver.impl.scripts;

import de.flogehring.peelserver.api.data.scripts.RunRequest;
import de.flogehring.peelserver.api.data.scripts.RunResponse;
import de.flogehring.peelserver.api.data.scripts.ValidationRequest;
import de.flogehring.peelserver.api.data.scripts.ValidationResponse;
import de.flogehring.peelserver.api.services.PeelScriptController;
import de.flogehring.peelserver.impl.project.ProjectAccessService;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.run.RunService;
import de.flogehring.peelserver.impl.run.ValidationService;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PeelScriptService implements PeelScriptController {
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
}
