package de.flogehring.peelserver.impl.project;
import de.flogehring.peelserver.impl.files.ProjectFileService;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import de.flogehring.peelserver.api.data.projects.ProjectData;
import de.flogehring.peelserver.api.data.projects.ProjectSummary;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationSummaryResponse;
import de.flogehring.peelserver.api.data.scripts.ScriptSummaryResponse;
import de.flogehring.peelserver.api.data.template.DocumentTemplateSummaryResponse;
import de.flogehring.peelserver.api.services.ProjectController;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProjectService implements ProjectController {

    private final ProjectFileService files;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccess;


    @Override
    public List<ProjectSummary> listProjects(SpringUserDetails springUserDetails) {
        return projectRepository.findAccessibleByUser(springUserDetails.getUserId())
                .stream()
                .filter(project -> project.getState() != ProjectState.DELETING || projectAccess.isOwner(project, springUserDetails.getUserId()))
                .map(project -> new ProjectSummary(
                        new ProjectIdApi(project.getId()),
                        project.getPersistenceData().projectName(),
                        project.getPersistenceData().projectDescription(), project.getState()
                ))
                .toList();
    }

    @Override
    public ProjectSummary createProject(String name, SpringUserDetails springUserDetails) {
        ProjectPersistence projectPersistence = ProjectPersistence.newProject(springUserDetails.getUserId(), name);
        projectRepository.save(projectPersistence);
        return new ProjectSummary(
                new ProjectIdApi(projectPersistence.getId()),
                projectPersistence.getPersistenceData().projectName(),
                projectPersistence.getPersistenceData().projectDescription(), projectPersistence.getState()
        );
    }

    @Override
    public ProjectData getProject(String projectId, SpringUserDetails springUserDetails) {
        ProjectPersistence projectPersistence = projectAccess.requireProjectAccess(new ProjectId(projectId), springUserDetails.getUserId());
        var summaries = files.list(projectId, null, springUserDetails);
        return new ProjectData(new ProjectIdApi(projectId), projectPersistence.getPersistenceData().projectName(), projectPersistence.getState(),
                summaries.stream().filter(file -> file.type().equals("script")).map(file -> new ScriptSummaryResponse(file.id(), file.name())).toList(),
                summaries.stream().filter(file -> file.type().equals("document")).map(file -> new DocumentTemplateSummaryResponse(file.id(), file.name(), file.updatedAt())).toList(),
                summaries.stream().filter(file -> file.type().equals("render-config")).map(file -> new RenderConfigurationSummaryResponse(file.id(), file.name())).toList());
    }
}
