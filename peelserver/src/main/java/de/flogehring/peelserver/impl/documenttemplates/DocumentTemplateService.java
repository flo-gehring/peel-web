package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.api.data.template.DocumentPreviewRequest;
import de.flogehring.peelserver.api.data.template.DocumentPreviewResponse;
import de.flogehring.peelserver.api.services.DocumentTemplateController;
import de.flogehring.peelserver.impl.files.FileContent;
import de.flogehring.peelserver.impl.files.ProjectFileRepository;
import de.flogehring.peelserver.impl.project.ProjectAccessService;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DocumentTemplateService implements DocumentTemplateController {
    private final ProjectFileRepository files;
    private final DocumentTemplateRenderService renderer;
    private final DocumentReferenceService references;
    private final ProjectAccessService projectAccess;

    @Override
    public DocumentPreviewResponse previewDocument(DocumentPreviewRequest request, SpringUserDetails user) {
        return projectAccess.active(new ProjectId(request.projectId().id()), user.getUserId(), () -> {
            var document = references.resolve(new ProjectId(request.projectId().id()), "Preview", request.document(), request.renderConfigId());
            return renderer.preview(document, request.bindings());
        });
    }

    @Override
    public DocumentPreviewResponse previewDocument(String id, Map<String, Object> bindings, SpringUserDetails user) {
        var file = files.findById(id).filter(value -> value.content() instanceof FileContent.DocumentContent).orElseThrow(ProjectAccessService::notFound);
        return projectAccess.active(file.projectId(), user.getUserId(), () -> {
            var fresh = files.findById(id).filter(value -> value.projectId().equals(file.projectId())).orElseThrow(ProjectAccessService::notFound);
            return renderer.preview(references.resolve(fresh), bindings);
        });
    }
}
