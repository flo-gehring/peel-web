package de.flogehring.peelserver;

import de.flogehring.peelserver.api.scripts.ScriptDtoResponse;
import de.flogehring.peelserver.api.scripts.ScriptSaveRequest;
import de.flogehring.peelserver.api.ScriptSummaryResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange("/api/scripts")
public interface PeelScriptController {

    @PostExchange
    ScriptDtoResponse saveScript(@RequestBody ScriptSaveRequest request);

    @GetExchange
    List<ScriptSummaryResponse> listScripts();

    @GetExchange("/{id}")
    ScriptDtoResponse getScript(@PathVariable String id);
}
