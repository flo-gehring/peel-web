package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(path = "/api/render-config", produces = MediaType.APPLICATION_JSON_VALUE)
public interface RenderConfigurationController {
    @GetMapping("/default")
    RenderConfigurationDto getDefault();
}
