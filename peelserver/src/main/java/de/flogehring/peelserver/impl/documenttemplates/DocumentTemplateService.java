package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.api.services.DocumentTemplateController;
import de.flogehring.peelserver.api.data.template.*;
import de.flogehring.peelserver.api.error.ResourceNotFoundException;
import de.flogehring.peelserver.impl.renderconfig.RenderConfigurationId;
import de.flogehring.peelserver.impl.renderconfig.RenderConfigurationRepository;
import de.flogehring.peelserver.impl.scripts.PeelScript;
import de.flogehring.peelserver.impl.scripts.PeelScriptId;
import de.flogehring.peelserver.impl.scripts.PeelScriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

import static de.flogehring.peelserver.impl.util.StreamUtil.transformMapValues;

@RestController
@RequiredArgsConstructor
public class DocumentTemplateService implements DocumentTemplateController {

    private final PeelDocumentRepository peelDocumentRepository;
    private final PeelScriptRepository peelScriptRepository;
    private final RenderConfigurationRepository renderConfigurationRepository;
    private final DocumentRenderService documentRenderService;

    @Override
    public DocumentTemplateSaveResponse saveDocument(DocumentTemplateSaveRequest request) {
        String templateHtml = requireTemplate(request.templateHtml());
        if (request.id() != null && !request.id().isBlank()) {
            Optional<DocumentPersistence> existing = peelDocumentRepository.findById(request.id());
            DocumentPersistence document = existing.orElseGet(() -> DocumentPersistence.newDocument(
                    request.id(),
                    request.name(),
                    transformMapValues(request.scriptNameTags(), PeelScriptId::new),
                    templateHtml,
                    request.editorStateJson(),
                    new RenderConfigurationId(request.renderConfigurationId())
            ));
            DocumentPersistence saved = peelDocumentRepository.save(
                    document.update(
                            request.name(),
                            transformMapValues(request.scriptNameTags(), PeelScriptId::new),
                            templateHtml,
                            request.editorStateJson(),
                            new RenderConfigurationId(request.renderConfigurationId())
                    ));
            return getSaveResponse(saved, existing.isPresent() ? DocumentTemplateSaveResponse.SaveType.UPDATED : DocumentTemplateSaveResponse.SaveType.CREATED);
        }
        DocumentPersistence created = DocumentPersistence.newDocument(
                UUID.randomUUID().toString(),
                request.name(),
                transformMapValues(request.scriptNameTags(), PeelScriptId::new),
                templateHtml,
                request.editorStateJson(),
                new RenderConfigurationId(request.renderConfigurationId())
        );
        DocumentPersistence saved = peelDocumentRepository.save(created);
        return getSaveResponse(saved, DocumentTemplateSaveResponse.SaveType.CREATED);
    }

    @Override
    public List<DocumentTemplateSummaryResponse> listDocuments() {
        return peelDocumentRepository.findAll().stream()
                .map(this::toSummary)
                .sorted(Comparator.comparing(DocumentTemplateSummaryResponse::updatedAt).reversed())
                .toList();
    }

    private static DocumentTemplateSaveResponse getSaveResponse(DocumentPersistence saved, DocumentTemplateSaveResponse.SaveType saveType) {
        return new DocumentTemplateSaveResponse(
                saved.getId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saveType
        );
    }

    @Override
    public DocumentTemplate getDocument(String id) {
        DocumentPersistence document = peelDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        return toContent(document);
    }

    @Override
    public DocumentPreviewResponse previewDocument(DocumentPreviewRequest request) {
        Document document = new Document(
                "PreviewDocumentRequest",
                request.scriptTags().entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        entry -> getPeelScript(new PeelScriptId(entry.getValue().id()))
                )),
                request.template(),
                renderConfigurationRepository.findById(request.renderConfigId())
                        .orElseThrow(() -> new ResourceNotFoundException("Render configuration not found: " + request.renderConfigId()))
                        .getExpressionRenderConfiguration()
        );
        String html = documentRenderService.render(document, request.bindings());
        return new DocumentPreviewResponse(html);
    }

    @Override
    public DocumentPreviewResponse previewDocument(String id, Map<String, Object> bindings) {
        DocumentPersistence documentPersistence = peelDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        DocumentPersistenceData data = documentPersistence.getData();
        Document document = new Document(
                data.name(),
                transformMapValues(data.scriptNameTags(), this::getPeelScript),
                documentPersistence.getData().templateHtml(),
                renderConfigurationRepository.findById(data.renderConfigurationId().id()).orElseThrow(() -> new ResourceNotFoundException("Render configuration not found: " + data.renderConfigurationId().id())).getExpressionRenderConfiguration()
        );
        String html = documentRenderService.render(document, bindings);
        return new DocumentPreviewResponse(html);
    }

    private PeelScript getPeelScript(PeelScriptId scriptId) {
        return peelScriptRepository.findById(scriptId.id())
                .orElseThrow(() -> new ResourceNotFoundException("Did not find Script with Id" + scriptId)).getPeelScript();
    }

    private DocumentTemplate toContent(DocumentPersistence document) {
        return new DocumentTemplate(
                document.getId(),
                document.getData().name(),
                transformMapValues(document.getData().scriptNameTags(), PeelScriptId::id),
                document.getData().templateHtml(),
                document.getData().editorStateJson(),
                document.getData().renderConfigurationId().id(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    @Override
    public void deleteDocument(String id) {
        peelDocumentRepository.deleteById(id);
    }

    private DocumentTemplateSummaryResponse toSummary(DocumentPersistence document) {
        return new DocumentTemplateSummaryResponse(
                document.getId(),
                document.getData().name(),
                document.getUpdatedAt()
        );
    }

    private String requireTemplate(String template) {
        if (template.isBlank()) {
            throw new IllegalArgumentException("template must not be blank");
        }
        return template;
    }
}
