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
import de.flogehring.peelserver.impl.users.UserId;
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


    @Override
    public List<ProjectSummary> listProjects(SpringUserDetails springUserDetails) {
        return projectRepository.findAll()
                .stream()
                .filter(project -> canReadProject(project, springUserDetails.getUserId()))
                .map(project -> new ProjectSummary(
                        new ProjectIdApi(project.getId()),
                        project.getPersistenceData().projectName()
                ))
                .toList();
    }

    @Override
    public ProjectSummary createProject(String name, SpringUserDetails springUserDetails) {
        ProjectPersistence projectPersistence = ProjectPersistence.newProject(springUserDetails.getUserId(), name);
        projectRepository.save(projectPersistence);
        return new ProjectSummary(
                new ProjectIdApi(projectPersistence.getId()),
                projectPersistence.getPersistenceData().projectName()
        );
    }

    @Override
    public ProjectData getProject(String projectId, SpringUserDetails springUserDetails) {
        ProjectPersistence projectPersistence = projectRepository.findById(projectId)
                .filter(project -> canReadProject(project, springUserDetails.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        ProjectId projectIdDomainValue = new ProjectId(projectPersistence.getId());
        return new ProjectData(
                new ProjectIdApi(projectPersistence.getId()),
                projectPersistence.getPersistenceData().projectName(),
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

    private static boolean canReadProject(ProjectPersistence project, UserId userId) {
        return project.getPersistenceData().ownerId().equals(userId) || project.getPersistenceData().members().contains(userId);
    }
}
