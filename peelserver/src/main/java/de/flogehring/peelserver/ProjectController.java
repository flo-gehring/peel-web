package de.flogehring.peelserver;

import de.flogehring.peelserver.api.projects.CreateProjectRequest;
import de.flogehring.peelserver.api.projects.IdeOpenUrlResponse;
import de.flogehring.peelserver.api.projects.ProjectDetailResponse;
import de.flogehring.peelserver.api.projects.ProjectListResponse;
import de.flogehring.peelserver.api.ScriptSummaryResponse;
import de.flogehring.peelserver.api.scripts.ProjectRunRequest;
import de.flogehring.peelserver.api.scripts.RunResponse;
import de.flogehring.peelserver.api.scripts.ScriptDtoResponse;
import de.flogehring.peelserver.api.scripts.ScriptSaveRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange("/api/projects")
public interface ProjectController {

    @GetExchange
    ProjectListResponse listProjects(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort
    );

    @PostExchange
    @ResponseStatus(HttpStatus.CREATED)
    ProjectDetailResponse createProject(@RequestBody CreateProjectRequest request);

    @GetExchange("/{projectId}")
    ProjectDetailResponse getProject(@PathVariable String projectId);

    @GetExchange("/{projectId}/ide-open-url")
    IdeOpenUrlResponse getIdeOpenUrl(@PathVariable String projectId);

    @GetExchange("/{projectId}/scripts")
    List<ScriptSummaryResponse> listProjectScripts(@PathVariable String projectId);

    @PostExchange("/{projectId}/scripts")
    ScriptDtoResponse saveProjectScript(@PathVariable String projectId, @RequestBody ScriptSaveRequest request);

    @GetExchange("/{projectId}/scripts/{scriptId}")
    ScriptDtoResponse getProjectScript(@PathVariable String projectId, @PathVariable String scriptId);

    @DeleteExchange("/{projectId}/scripts/{scriptId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteProjectScript(@PathVariable String projectId, @PathVariable String scriptId);

    @PostExchange("/{projectId}/run")
    RunResponse runProjectScript(@PathVariable String projectId, @RequestBody ProjectRunRequest request);
}
