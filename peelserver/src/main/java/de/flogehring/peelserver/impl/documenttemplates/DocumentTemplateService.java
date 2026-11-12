package de.flogehring.peelserver.impl.documenttemplates;
import de.flogehring.peelserver.api.data.template.*;
import de.flogehring.peelserver.api.services.DocumentTemplateController;
import de.flogehring.peelserver.impl.files.*;
import de.flogehring.peelserver.impl.project.*;
import de.flogehring.peelserver.impl.scripts.PeelScriptId;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.stream.Collectors;
@RestController @RequiredArgsConstructor
public class DocumentTemplateService implements DocumentTemplateController {
    private final ProjectFileRepository files;
    private final DocumentTemplateRenderService renderer;
    private final DocumentReferenceService references;
    private final ProjectAccessService projectAccess;
    @Override
    public DocumentPreviewResponse previewDocument(DocumentPreviewRequest request, SpringUserDetails user) {
        return projectAccess.active(new ProjectId(request.projectId().id()), user.getUserId(), () -> {
            var tags = request.scriptTags().entrySet().stream().collect(Collectors.toMap(
                    entry -> entry.getKey().name(), entry -> new PeelScriptId(entry.getValue().id())));
            var document = references.resolve(new ProjectId(request.projectId().id()), "Preview", tags, request.template(), request.renderConfigId());
            return new DocumentPreviewResponse(renderer.renderPdf(document, request.bindings(), ByteArrayOutputStream::toByteArray));
        });
    }
    @Override
    public DocumentPreviewResponse previewDocument(String id, Map<String, Object> bindings, SpringUserDetails user) {
        var file = files.findById(id).filter(value -> value.content() instanceof FileContent.DocumentContent).orElseThrow(ProjectAccessService::notFound);
        return projectAccess.active(file.projectId(), user.getUserId(), () -> {
            var fresh = files.findById(id).filter(value -> value.projectId().equals(file.projectId())).orElseThrow(ProjectAccessService::notFound);
            return new DocumentPreviewResponse(renderer.renderPdf(references.resolve(fresh), bindings, ByteArrayOutputStream::toByteArray));
        });
    }
}
