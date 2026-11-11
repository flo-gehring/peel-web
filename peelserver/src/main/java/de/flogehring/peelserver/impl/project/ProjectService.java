package de.flogehring.peelserver.impl.project;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import de.flogehring.peelserver.api.data.projects.ProjectData;
import de.flogehring.peelserver.api.data.projects.ProjectSummary;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationSummaryResponse;
import de.flogehring.peelserver.api.data.scripts.ScriptSummaryResponse;
import de.flogehring.peelserver.api.data.template.DocumentTemplateSummaryResponse;
import de.flogehring.peelserver.api.services.ProjectController;
import de.flogehring.peelserver.impl.documenttemplates.PeelDocumentRepository;
import de.flogehring.peelserver.impl.renderconfig.RenderConfigurationRepository;
import de.flogehring.peelserver.impl.scripts.PeelScriptRepository;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProjectService implements ProjectController {

    private final PeelScriptRepository peelScriptRepository;
    private final ProjectRepository projectRepository;
    private final RenderConfigurationRepository renderConfigurationRepository;
    private final PeelDocumentRepository peelDocumentRepository;
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
        ProjectId projectIdDomainValue = new ProjectId(projectPersistence.getId());
        return new ProjectData(
                new ProjectIdApi(projectPersistence.getId()),
                projectPersistence.getPersistenceData().projectName(),
                projectPersistence.getState(),
                peelScriptRepository.findByProjectId(projectIdDomainValue).stream()
                        .map(script -> new ScriptSummaryResponse(
                                script.getId(),
                                script.getPeelScript().description()
                        ))
                        .toList(),
                peelDocumentRepository.findByProjectId(projectIdDomainValue).stream()
                        .map(document -> new DocumentTemplateSummaryResponse(
                                document.getId(),
                                document.getData().name(),
                                document.getUpdatedAt()
                        ))
                        .toList(),
                renderConfigurationRepository.findByProjectId(projectIdDomainValue).stream()
                        .map(renderConfig -> new RenderConfigurationSummaryResponse(
                                renderConfig.getId(),
                                renderConfig.getName()
                        ))
                        .toList()
        );
    }

}
