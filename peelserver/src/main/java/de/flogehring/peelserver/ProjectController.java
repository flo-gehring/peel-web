package de.flogehring.peelserver;

import de.flogehring.peelserver.api.projects.CreateProjectRequest;
import de.flogehring.peelserver.api.projects.IdeOpenUrlResponse;
import de.flogehring.peelserver.api.projects.ProjectDetailResponse;
import de.flogehring.peelserver.api.projects.ProjectListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

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
}
