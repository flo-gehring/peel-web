package de.flogehring.peelserver.impl.files;

import de.flogehring.peelserver.api.data.files.FileData.*;
import de.flogehring.peelserver.impl.project.*;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectFileService {
    private final ProjectFileRepository files;
    private final ProjectAccessService access;
    private final MongoTemplate mongo;

    public List<FileSummary> list(String projectId, String type, SpringUserDetails user) {
        var project = new ProjectId(projectId);
        access.requireProjectAccess(project, user.getUserId());
        if (type != null && !Set.of("script", "render-config", "document").contains(type))
            throw new IllegalArgumentException("Unknown file type");
        var criteria = Criteria.where("projectId").is(project);
        if (type != null) criteria.and("content._class").is(type);
        var query = Query.query(criteria);
        query.fields().include("id", "name", "description", "version", "content._class", "createdAt", "updatedAt");
        // Read only metadata and the discriminator, without constructing incomplete content records.
        return mongo.find(query, org.bson.Document.class, "project-files").stream().map(doc ->
                new FileSummary(doc.getString("_id"), doc.getString("name"), doc.getString("description"),
                        ((Number) doc.get("version")).longValue(), doc.get("content", org.bson.Document.class).getString("_class"),
                        doc.getDate("createdAt").toInstant(), doc.getDate("updatedAt").toInstant())).toList();
    }

    public ProjectFile get(String projectId, String id, SpringUserDetails user) {
        access.requireProjectAccess(new ProjectId(projectId), user.getUserId());
        return requireFile(new ProjectId(projectId), id);
    }

    public ProjectFile create(String projectId, CreateFile request, SpringUserDetails user) {
        var project = new ProjectId(projectId);
        return access.active(project, user.getUserId(), () -> {
            String id = request.id().toString();
            String name = request.name().strip();
            String description = description(request.description());
            var existing = files.findById(id);
            if (existing.isPresent()) return repeatedCreate(project, existing.get(), name, description, request.content());
            validateReferences(project, request.content());
            Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            try {
                return files.insert(new ProjectFile(id, project, name, description, 0, request.content(), now, now));
            } catch (DuplicateKeyException exception) {
                var concurrent = files.findById(id);
                if (concurrent.isPresent()) return repeatedCreate(project, concurrent.get(), name, description, request.content());
                throw nameConflict();
            }
        });
    }

    public ProjectFile update(String projectId, String id, UpdateFile request, SpringUserDetails user) {
        var project = new ProjectId(projectId);
        return access.active(project, user.getUserId(), () -> {
            var existing = requireFile(project, id);
            String name = request.name().strip();
            String description = description(request.description());
            if (sameState(existing, name, description, request.content())) return existing;
            if (existing.version() != request.expectedVersion()) throw versionConflict(existing);
            if (!existing.content().kind().equals(request.content().kind()))
                throw new IllegalArgumentException("A file's type cannot be changed");
            validateReferences(project, request.content());
            var query = Query.query(Criteria.where("id").is(id).and("projectId").is(project).and("version").is(request.expectedVersion()));
            var update = new Update().set("name", name).set("description", description).set("content", request.content())
                    .set("updatedAt", Instant.now()).inc("version", 1);
            try {
                var saved = mongo.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), ProjectFile.class);
                if (saved != null) return saved;
            } catch (DuplicateKeyException exception) { throw nameConflict(); }
            var latest = requireFile(project, id);
            if (sameState(latest, name, description, request.content())) return latest;
            throw versionConflict(latest);
        });
    }

    public void delete(String projectId, String id, long expectedVersion, SpringUserDetails user) {
        var project = new ProjectId(projectId);
        access.active(project, user.getUserId(), () -> {
            var existing = files.findById(id).filter(file -> project.equals(file.projectId()));
            if (existing.isEmpty()) return null;
            if (existing.get().version() != expectedVersion) throw versionConflict(existing.get());
            var uses = files.findByProjectId(project).stream().filter(file -> file.content() instanceof FileContent.DocumentContent document
                    && (document.renderConfigurationId().equals(id) || document.scriptNameTags().containsValue(id)))
                    .map(FileSummary::of).toList();
            if (!uses.isEmpty()) throw new FileConflictException("file_in_use", "Remove file references before deleting this file.", Map.of("usages", uses));
            var result = mongo.remove(Query.query(Criteria.where("id").is(id).and("projectId").is(project)
                    .and("version").is(expectedVersion)), ProjectFile.class);
            if (result.getDeletedCount() == 0) {
                var latest = files.findById(id).filter(file -> project.equals(file.projectId()));
                if (latest.isPresent()) throw versionConflict(latest.get());
            }
            return null;
        });
    }

    public ProjectFile requireFile(ProjectId project, String id) {
        return files.findById(id).filter(file -> project.equals(file.projectId())).orElseThrow(ProjectAccessService::notFound);
    }

    private ProjectFile repeatedCreate(ProjectId project, ProjectFile existing, String name, String description, FileContent content) {
        if (!project.equals(existing.projectId())) throw ProjectAccessService.notFound();
        if (existing.version() == 0 && sameState(existing, name, description, content)) return existing;
        throw new FileConflictException("file_already_exists", "This file ID already exists. Creation never overwrites a file.", Map.of("currentVersion", existing.version()));
    }

    private void validateReferences(ProjectId project, FileContent content) {
        if (content instanceof FileContent.DocumentContent document) {
            if (!(requireFile(project, document.renderConfigurationId()).content() instanceof FileContent.RenderConfigContent))
                throw new IllegalArgumentException("The render configuration reference must point to a render configuration");
            var ids = new HashSet<>(document.scriptNameTags().values());
            var referenced = files.findAllById(ids);
            if (referenced.size() != ids.size()) throw ProjectAccessService.notFound();
            for (var file : referenced) {
                if (!project.equals(file.projectId())) throw ProjectAccessService.notFound();
                if (!(file.content() instanceof FileContent.ScriptContent)) throw new IllegalArgumentException("Script references must point to scripts");
            }
        }
    }

    private static String description(String description) { return description == null ? "" : description; }
    private static boolean sameState(ProjectFile file, String name, String description, FileContent content) {
        return file.name().equals(name) && file.description().equals(description) && file.content().equals(content);
    }
    private static FileConflictException versionConflict(ProjectFile file) {
        return new FileConflictException("file_version_conflict", "The file changed. Reload and merge your changes before saving.", Map.of("currentVersion", file.version()));
    }
    private static FileConflictException nameConflict() {
        return new FileConflictException("file_name_conflict", "A file of this type with this name already exists in the project.", Map.of());
    }
}
