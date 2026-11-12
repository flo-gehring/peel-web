package de.flogehring.peelserver.impl.documenttemplates;
import de.flogehring.peelserver.impl.files.*;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.scripts.PeelScript;
import de.flogehring.peelserver.impl.scripts.PeelScriptId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import static de.flogehring.peelserver.impl.project.ProjectAccessService.notFound;
@Service @RequiredArgsConstructor
public class DocumentReferenceService {
    private final ProjectFileRepository files;
    public Document resolve(ProjectId project, String name, Map<String, PeelScriptId> tags, String template, String configId) {
        if (project == null || tags == null || configId == null || configId.isBlank()
                || tags.values().stream().anyMatch(id -> id == null || id.id() == null || id.id().isBlank())) throw notFound();
        var config = files.findById(configId).filter(value -> project.equals(value.projectId())
                && value.content() instanceof FileContent.RenderConfigContent).orElseThrow(() -> notFound());
        Set<String> ids = tags.values().stream().map(PeelScriptId::id).collect(Collectors.toSet());
        Map<String, PeelScript> byId = new HashMap<>();
        files.findAllById(ids).forEach(file -> {
            if (!project.equals(file.projectId()) || !(file.content() instanceof FileContent.ScriptContent script)) throw notFound();
            byId.put(file.id(), new PeelScript(file.name(), script.source()));
        });
        if (byId.size() != ids.size()) throw notFound();
        Map<String, PeelScript> resolved = new HashMap<>();
        tags.forEach((tag, id) -> resolved.put(tag, byId.get(id.id())));
        return new Document(name, resolved, template, ((FileContent.RenderConfigContent) config.content()).configuration());
    }
    public Document resolve(ProjectFile file) {
        if (!(file.content() instanceof FileContent.DocumentContent data)) throw notFound();
        var tags = data.scriptNameTags().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new PeelScriptId(entry.getValue())));
        return resolve(file.projectId(), file.name(), tags, data.templateHtml(), data.renderConfigurationId());
    }
}
