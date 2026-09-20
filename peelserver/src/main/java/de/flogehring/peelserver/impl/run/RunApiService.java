package de.flogehring.peelserver.impl.run;

import de.flogehring.peelserver.api.services.RunController;
import de.flogehring.peelserver.api.data.scripts.RunRequest;
import de.flogehring.peelserver.api.data.scripts.RunResponse;
import de.flogehring.peelserver.api.data.scripts.ValidationRequest;
import de.flogehring.peelserver.api.data.scripts.ValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RunApiService implements RunController {

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
}
