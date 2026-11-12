package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationDto;
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

@RequestMapping(path = "/api/render-config", produces = MediaType.APPLICATION_JSON_VALUE)
public interface RenderConfigurationController {
    @GetMapping("/default")
    RenderConfigurationDto getDefault();
}
