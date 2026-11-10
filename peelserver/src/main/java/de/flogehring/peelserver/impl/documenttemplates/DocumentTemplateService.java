package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.api.data.template.*;
import de.flogehring.peelserver.api.services.DocumentTemplateController;
import de.flogehring.peelserver.impl.project.ProjectAccessService;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.renderconfig.RenderConfigurationId;
import de.flogehring.peelserver.impl.scripts.PeelScriptId;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import static de.flogehring.peelserver.impl.util.StreamUtil.transformMapValues;

@RestController
@RequiredArgsConstructor
public class DocumentTemplateService implements DocumentTemplateController {
    private final PeelDocumentRepository documents;
    private final DocumentTemplateRenderService renderer;
    private final DocumentReferenceService references;
    private final ProjectAccessService projectAccess;

    @Override
    public DocumentTemplateSaveResponse saveDocument(DocumentTemplateSaveRequest request, SpringUserDetails user) {
        ProjectId projectId = new ProjectId(request.projectId().id());
        projectAccess.requireProjectAccess(projectId, user.getUserId());
        String template = requireTemplate(request.templateHtml());
        var tags = transformMapValues(request.scriptNameTags(), PeelScriptId::new);
        references.resolve(projectId, request.name(), tags, template, request.renderConfigurationId());
        var created = DocumentTemplatePersistence.newDocument(UUID.randomUUID().toString(), request.name(), tags,
                template, request.editorStateJson(), new RenderConfigurationId(request.renderConfigurationId()), projectId);
        return response(documents.save(created), DocumentTemplateSaveResponse.SaveType.CREATED);
    }

    @Override
    public DocumentTemplateSaveResponse updateDocument(String id, DocumentTemplateUpdateRequest request, SpringUserDetails user) {
        var document = requireDocumentAccess(id, user);
        String template = requireTemplate(request.templateHtml());
        var tags = transformMapValues(request.scriptNameTags(), PeelScriptId::new);
        references.resolve(document.getProjectId(), request.name(), tags, template, request.renderConfigurationId());
        var updated = document.update(request.name(), tags, template, request.editorStateJson(), new RenderConfigurationId(request.renderConfigurationId()));
        return response(documents.save(updated), DocumentTemplateSaveResponse.SaveType.UPDATED);
    }

    @Override
    public DocumentTemplateDto getDocument(String id, SpringUserDetails user) {
        var document = requireDocumentAccess(id, user);
        var data = document.getData();
        return new DocumentTemplateDto(document.getId(), data.name(), transformMapValues(data.scriptNameTags(), PeelScriptId::id),
                data.templateHtml(), data.editorStateJson(), data.renderConfigurationId().id(), document.getCreatedAt(), document.getUpdatedAt());
    }

    @Override
    public void deleteDocument(String id, SpringUserDetails user) {
        requireDocumentAccess(id, user);
        documents.deleteById(id);
    }

    @Override
    public DocumentPreviewResponse previewDocument(DocumentPreviewRequest request, SpringUserDetails user) {
        ProjectId projectId = new ProjectId(request.projectId().id());
        projectAccess.requireProjectAccess(projectId, user.getUserId());
        var tags = request.scriptTags().entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().name(), entry -> new PeelScriptId(entry.getValue().id())));
        Document document = references.resolve(projectId, "Preview", tags, request.template(), request.renderConfigId());
        return new DocumentPreviewResponse(renderer.renderPdf(document, request.bindings(), ByteArrayOutputStream::toByteArray));
    }

    @Override
    public DocumentPreviewResponse previewDocument(String id, Map<String, Object> bindings, SpringUserDetails user) {
        Document document = references.resolve(requireDocumentAccess(id, user));
        return new DocumentPreviewResponse(renderer.renderPdf(document, bindings, ByteArrayOutputStream::toByteArray));
    }

    private DocumentTemplatePersistence requireDocumentAccess(String id, SpringUserDetails user) {
        var document = documents.findById(id).orElseThrow(ProjectAccessService::notFound);
        projectAccess.requireProjectAccess(document.getProjectId(), user.getUserId());
        return document;
    }

    private DocumentTemplateSaveResponse response(DocumentTemplatePersistence document, DocumentTemplateSaveResponse.SaveType saveType) {
        return new DocumentTemplateSaveResponse(document.getId(), document.getCreatedAt(), document.getUpdatedAt(), saveType);
    }

    private String requireTemplate(String template) {
        if (template == null || template.isBlank()) throw new IllegalArgumentException("template must not be blank");
        return template;
    }
}
