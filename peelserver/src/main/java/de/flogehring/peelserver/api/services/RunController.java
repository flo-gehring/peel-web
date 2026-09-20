package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.scripts.RunRequest;
import de.flogehring.peelserver.api.data.scripts.RunResponse;
import de.flogehring.peelserver.api.data.scripts.ValidationRequest;
import de.flogehring.peelserver.api.data.scripts.ValidationResponse;
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
