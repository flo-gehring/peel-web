package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.scripts.RunRequest;
import de.flogehring.peelserver.api.data.scripts.RunResponse;
import de.flogehring.peelserver.api.data.scripts.ValidationRequest;
import de.flogehring.peelserver.api.data.scripts.ValidationResponse;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(path = "/api/scripts", produces = MediaType.APPLICATION_JSON_VALUE)
public interface PeelScriptController {
    @PutMapping(path = "/run", consumes = MediaType.APPLICATION_JSON_VALUE)
    RunResponse run(@Valid @RequestBody RunRequest request, @AuthenticationPrincipal SpringUserDetails user);

    @GetMapping("/validate")
    ValidationResponse validate(@RequestBody ValidationRequest request);
}
