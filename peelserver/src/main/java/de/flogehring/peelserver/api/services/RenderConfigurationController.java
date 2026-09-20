package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.renderconfig.IdNameTuple;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationCreateResponse;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationDto;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationPersistenceDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping(path = "/api/render-config", produces = MediaType.APPLICATION_JSON_VALUE)
public interface RenderConfigurationController {

    @PostMapping(path = "/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    RenderConfigurationCreateResponse createRenderConfiguration(@RequestBody RenderConfigurationPersistenceDto renderConfigurationDto);

    @PutMapping(path = "/update/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void updateRenderConfiguration(@PathVariable(value = "id") String id, @RequestBody RenderConfigurationPersistenceDto renderConfigurationDto);

    @GetMapping("/default")
    RenderConfigurationDto getDefault();

    @GetMapping("/{id}")
    RenderConfigurationPersistenceDto getById(@PathVariable(value = "id") String id);

    @DeleteMapping("/{id}")
    void deleteById(@PathVariable(value = "id") String id);

    @GetMapping("/list-ids")
    List<IdNameTuple> listAll();
}
