package de.flogehring.peelserver;

import de.flogehring.peelserver.api.scripts.RunRequest;
import de.flogehring.peelserver.api.scripts.RunResponse;
import de.flogehring.peelserver.api.ValidationRequest;
import de.flogehring.peelserver.api.ValidationResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api")
public interface RunController {

    @PostExchange("/run")
    RunResponse run(@RequestBody RunRequest request);

    @PostExchange("/validate")
    ValidationResponse validate(@RequestBody ValidationRequest request);
}
