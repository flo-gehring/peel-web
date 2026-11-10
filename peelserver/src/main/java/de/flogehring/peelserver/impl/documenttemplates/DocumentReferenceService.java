package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.renderconfig.RenderConfigurationRepository;
import de.flogehring.peelserver.impl.scripts.PeelScript;
import de.flogehring.peelserver.impl.scripts.PeelScriptId;
import de.flogehring.peelserver.impl.scripts.PeelScriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static de.flogehring.peelserver.impl.project.ProjectAccessService.notFound;

/** Resolves a document only from references in its own project, including for stored previews and print jobs. */
@Service
@RequiredArgsConstructor
public class DocumentReferenceService {
    private final PeelScriptRepository scripts;
    private final RenderConfigurationRepository configs;

    public Document resolve(ProjectId project, String name, Map<String, PeelScriptId> tags, String template, String configId) {
        if (project == null || tags == null || configId == null || configId.isBlank()
                || tags.values().stream().anyMatch(id -> id == null || id.id() == null || id.id().isBlank())) throw notFound();
        var config = configs.findById(configId).filter(value -> project.equals(value.getProjectId())).orElseThrow(() -> notFound());
        Set<String> ids = tags.values().stream().map(PeelScriptId::id).collect(Collectors.toSet());
        Map<String, PeelScript> byId = new HashMap<>();
        scripts.findAllById(ids).forEach(script -> {
            if (!project.equals(script.getProjectId())) throw notFound();
            byId.put(script.getId(), script.getPeelScript());
        });
        if (byId.size() != ids.size()) throw notFound();
        Map<String, PeelScript> resolved = new HashMap<>();
        tags.forEach((tag, id) -> resolved.put(tag, byId.get(id.id())));
        return new Document(name, resolved, template, config.getExpressionRenderConfiguration());
    }

    public Document resolve(DocumentTemplatePersistence document) {
        var data = document.getData();
        return resolve(document.getProjectId(), data.name(), data.scriptNameTags(), data.templateHtml(), data.renderConfigurationId().id());
    }
}
