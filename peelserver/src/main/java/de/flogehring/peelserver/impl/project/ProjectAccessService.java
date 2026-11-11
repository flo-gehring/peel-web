package de.flogehring.peelserver.impl.project;

import de.flogehring.peelserver.api.error.ResourceNotFoundException;
import de.flogehring.peelserver.impl.users.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {
    private final ProjectRepository projects;
    // Serializes project changes in this application's single backend instance, with bounded lock storage.
    private final ReentrantLock[] locks = IntStream.range(0, 128).mapToObj(i -> new ReentrantLock()).toArray(ReentrantLock[]::new);

    public ProjectPersistence requireProjectAccess(ProjectId projectId, UserId userId) {
        if (projectId == null || projectId.id() == null || projectId.id().isBlank()) throw notFound();
        return projects.findAccessibleById(projectId.id(), userId)
                .filter(project -> project.getState() != ProjectState.DELETING).orElseThrow(ProjectAccessService::notFound);
    }

    public List<ProjectId> accessibleProjectIds(UserId userId) {
        return projects.findAccessibleByUser(userId).stream().filter(project -> project.getState() != ProjectState.DELETING)
                .map(project -> new ProjectId(project.getId())).toList();
    }

    public static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Resource not found");
    }

    public <T> T locked(ProjectId id, Supplier<T> action) {
        var lock = locks[Math.floorMod(id.id().hashCode(), locks.length)];
        lock.lock();
        try { return action.get(); } finally { lock.unlock(); }
    }

    public <T> T active(ProjectId id, UserId user, Supplier<T> action) {
        return locked(id, () -> {
            requireActive(requireProjectAccess(id, user));
            return action.get();
        });
    }

    public void requireActive(ProjectPersistence project) {
        if (project.getState() != ProjectState.ACTIVE) throw new ResponseStatusException(HttpStatus.CONFLICT, "This project is archived and read-only.");
    }

    public boolean isOwner(ProjectPersistence project, UserId user) { return project.getPersistenceData().ownerId().equals(user); }
    public boolean isAdmin(ProjectPersistence project, UserId user) {
        return isOwner(project, user) || project.getPersistenceData().admins().contains(user);
    }
    public void requireOwner(ProjectPersistence project, UserId user) {
        if (!isOwner(project, user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the project owner can perform this action.");
    }
    public void requireAdmin(ProjectPersistence project, UserId user) {
        if (!isAdmin(project, user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An owner or admin is required.");
    }
}
