package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.api.data.template.DocumentNode;
import de.flogehring.peelserver.impl.files.FileContent;
import de.flogehring.peelserver.impl.files.ProjectFile;
import de.flogehring.peelserver.impl.files.ProjectFileRepository;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.impl.scripts.PeelScript;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.flogehring.peelserver.impl.project.ProjectAccessService.notFound;

@Service
@RequiredArgsConstructor
public class DocumentReferenceService {
    private final ProjectFileRepository files;

    public Document resolve(ProjectId project, String name, DocumentNode document, String configId) {
        if (project == null || document == null) throw new IllegalArgumentException("A document is required");
        document.validateDocument();
        ExpressionRenderConfiguration configuration = ExpressionRenderConfiguration.defaultConfig();
        if (configId != null && !configId.isBlank()) {
            var config = files.findById(configId).filter(value -> project.equals(value.projectId())
                    && value.content() instanceof FileContent.RenderConfigContent).orElseThrow(() -> notFound());
            configuration = ((FileContent.RenderConfigContent) config.content()).configuration();
        }
        Set<String> ids = document.scriptIds();
        Map<String, PeelScript> byId = new HashMap<>();
        files.findAllById(ids).forEach(file -> {
            if (!project.equals(file.projectId()) || !(file.content() instanceof FileContent.ScriptContent script))
                throw notFound();
            byId.put(file.id(), new PeelScript(file.name(), script.source(), script.expectedBindings()));
        });
        if (byId.size() != ids.size()) throw notFound();
        return new Document(name, Map.copyOf(byId), document, configuration);
    }

    public Document resolve(ProjectFile file) {
        if (!(file.content() instanceof FileContent.DocumentContent data)) throw notFound();
        return resolve(file.projectId(), file.name(), data.document(), data.renderConfigurationId());
    }
}
