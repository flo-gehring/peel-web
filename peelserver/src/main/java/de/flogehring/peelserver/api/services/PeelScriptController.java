package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.scripts.*;
import org.springframework.http.MediaType;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(path = "/api/scripts", produces = MediaType.APPLICATION_JSON_VALUE)
public interface PeelScriptController {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ScriptDtoResponse saveScript(@Valid @RequestBody CreateScriptRequest request, @AuthenticationPrincipal SpringUserDetails user);

    @PutMapping("/{id}")
    ScriptDtoResponse updateScript(@PathVariable String id, @RequestBody ScriptUpdateRequest request, @AuthenticationPrincipal SpringUserDetails user);

    @GetMapping("/{id}")
    ScriptDtoResponse getScript(@PathVariable String id, @AuthenticationPrincipal SpringUserDetails user);

    @DeleteMapping("/{id}")
    void deleteScript(@PathVariable String id, @AuthenticationPrincipal SpringUserDetails user);

    @PutMapping(path = "/run", consumes = MediaType.APPLICATION_JSON_VALUE)
    RunResponse run(@RequestBody RunRequest request);

    @GetMapping("/validate")
    ValidationResponse validate(@RequestBody ValidationRequest request);
}
