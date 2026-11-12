package de.flogehring.peelserver.impl.scripts;
import de.flogehring.peelserver.api.data.scripts.*;
import de.flogehring.peelserver.api.services.PeelScriptController;
import de.flogehring.peelserver.impl.project.*;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import de.flogehring.peelserver.impl.run.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequiredArgsConstructor
public class PeelScriptService implements PeelScriptController {
    private final ProjectAccessService projectAccess;
    private final RunService runService;
    private final ValidationService validationService;
    @Override
    public RunResponse run(RunRequest request, SpringUserDetails user) {
        return projectAccess.active(new ProjectId(request.projectId().id()), user.getUserId(), () -> runService.run(request));
    }
    @Override
    public ValidationResponse validate(ValidationRequest request) { return validationService.validate(request); }
}
