package de.flogehring.peelserver;

import de.flogehring.peelserver.api.IdNameTuple;
import de.flogehring.peelserver.api.RenderConfigurationCreateResponse;
import de.flogehring.peelserver.api.RenderConfigurationDto;
import de.flogehring.peelserver.api.RenderConfigurationPersistenceDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.List;

@HttpExchange("api/render-config")
public interface RenderConfigurationService {

    @PostExchange("save")
    RenderConfigurationCreateResponse createRenderConfiguration(@RequestBody RenderConfigurationPersistenceDto renderConfigurationDto);

    @PutExchange("update/{id}")
    void updateRenderConfiguration(@PathVariable(value = "id") String id, @RequestBody RenderConfigurationPersistenceDto renderConfigurationDto);

    @GetExchange("default")
    RenderConfigurationDto getDefault();

    @GetExchange("{id}")
    RenderConfigurationPersistenceDto getId(@PathVariable(value = "id") String id);

    @GetExchange("list-ids")
    List<IdNameTuple> listAll();
}
