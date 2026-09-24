package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.projects.ProjectData;
import de.flogehring.peelserver.api.data.projects.ProjectSummary;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping(path = "/api/projects", produces = MediaType.APPLICATION_JSON_VALUE)
public interface ProjectController {

    @GetMapping("/list")
    List<ProjectSummary> listProjects(@AuthenticationPrincipal SpringUserDetails springUserDetails);

    @PostMapping(path = "/{projectId:.+}")
    ProjectSummary createProject(@PathVariable("projectId") String name, @AuthenticationPrincipal SpringUserDetails springUserDetails);

    @GetMapping("/{projectId:.+}")
    ProjectData getProject(@PathVariable("projectId") String projectId, @AuthenticationPrincipal SpringUserDetails springUserDetails);
}
