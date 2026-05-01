package de.flogehring.peelserver.run;

import de.flogehring.peelserver.RunController;
import de.flogehring.peelserver.api.RunRequest;
import de.flogehring.peelserver.api.RunResponse;
import de.flogehring.peelserver.api.ValidationRequest;
import de.flogehring.peelserver.api.ValidationResponse;
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
