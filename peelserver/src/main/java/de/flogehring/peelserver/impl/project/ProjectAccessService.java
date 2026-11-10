package de.flogehring.peelserver.impl.project;

import de.flogehring.peelserver.api.error.ResourceNotFoundException;
import de.flogehring.peelserver.impl.users.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {
    private final ProjectRepository projects;

    public ProjectPersistence requireProjectAccess(ProjectId projectId, UserId userId) {
        if (projectId == null || projectId.id() == null || projectId.id().isBlank()) throw notFound();
        return projects.findAccessibleById(projectId.id(), userId).orElseThrow(ProjectAccessService::notFound);
    }

    public List<ProjectId> accessibleProjectIds(UserId userId) {
        return projects.findAccessibleByUser(userId).stream().map(project -> new ProjectId(project.getId())).toList();
    }

    public static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Resource not found");
    }
}
