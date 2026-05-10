package de.flogehring.peelserver.documents;

import de.flogehring.peelserver.DocumentController;
import de.flogehring.peelserver.api.RenderConfigurationDto;
import de.flogehring.peelserver.api.document.*;
import de.flogehring.peelserver.error.ResourceNotFoundException;
import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.renderconfig.RenderConfigurationId;
import de.flogehring.peelserver.renderconfig.RenderConfigurationRepository;
import de.flogehring.peelserver.scripts.PeelScript;
import de.flogehring.peelserver.scripts.PeelScriptId;
import de.flogehring.peelserver.scripts.PeelScriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.flogehring.peelserver.util.StreamUtil.transformMapValues;

@RestController
@RequiredArgsConstructor
public class DocumentService implements DocumentController {

    private final PeelDocumentRepository peelDocumentRepository;
    private final PeelScriptRepository peelScriptRepository;
    private final RenderConfigurationRepository renderConfigurationRepository;
    private final DocumentRenderService documentRenderService;

    @Override
    public DocumentSaveResponse saveDocument(DocumentSaveRequest request) {
        String template = requireTemplate(request.template());
        if (!request.id().isBlank()) {
            DocumentPersistence existing = peelDocumentRepository.findById(request.id())
                    .orElseGet(() -> DocumentPersistence.newDocument(
                            request.id(),
                            request.name(),
                            transformMapValues(request.scriptNameTags(), PeelScriptId::new),
                            request.template(),
                            new RenderConfigurationId(request.renderConfigurationId()),
                            toExpressionRenderConfig(request.localOverrides())
                    ));
            DocumentPersistence saved = peelDocumentRepository.save(
                    existing.update(
                            request.name(),
                            transformMapValues(request.scriptNameTags(), PeelScriptId::new),
                            template,
                            new RenderConfigurationId(request.renderConfigurationId()),
                            toExpressionRenderConfig(request.localOverrides())
                    ));
            return getSaveResponse(saved, DocumentSaveResponse.SaveType.CREATED);
        }
        DocumentPersistence created = DocumentPersistence.newDocument(
                UUID.randomUUID().toString(),
                request.name(),
                transformMapValues(request.scriptNameTags(), PeelScriptId::new),
                template,
                new RenderConfigurationId(request.renderConfigurationId()),
                toExpressionRenderConfig(request.localOverrides())
        );
        DocumentPersistence saved = peelDocumentRepository.save(created);
        return getSaveResponse(saved, DocumentSaveResponse.SaveType.UPDATED);
    }

    @Override
    public List<DocumentSummaryResponse> listDocuments() {
        return peelDocumentRepository.findAll().stream()
                .map(this::toSummary)
                .sorted(Comparator.comparing(DocumentSummaryResponse::updatedAt).reversed())
                .toList();
    }

    private static DocumentSaveResponse getSaveResponse(DocumentPersistence saved, DocumentSaveResponse.SaveType saveType) {
        return new DocumentSaveResponse(
                saved.getId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saveType
        );
    }

    @Override
    public DocumentContent getDocument(String id) {
        DocumentPersistence document = peelDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        return toContent(document);
    }

    @Override
    public DocumentPreviewResponse previewDocument(DocumentPreviewRequest request) {
        Document document = new Document(
                "PreviewDocumentRequest",
                request.scripTags().entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        entry -> getPeelScript(new PeelScriptId(entry.getValue().id()))
                )),
                request.template(),
                renderConfigurationRepository.findById(request.renderConfigId())
                        .orElseThrow(() -> new ResourceNotFoundException("Render configuration not found: " + request.renderConfigId()))
                        .getExpressionRenderConfiguration(),
                toExpressionRenderConfig(request.localOverrides())
        );
        String html = documentRenderService.render(document, request.bindings());
        return new DocumentPreviewResponse(html);
    }

    public static ExpressionRenderConfiguration toExpressionRenderConfig(RenderConfigurationDto renderConfigurationDto) {
        return ExpressionRenderConfiguration.of(renderConfigurationDto.renderConfigurations());
    }

    @Override
    public DocumentPreviewResponse previewDocument(String id, Map<String, Object> bindings) {
        DocumentPersistence documentPersistence = peelDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        DocumentPersistenceData data = documentPersistence.getData();
        Document document = new Document(
                data.name(),
                transformMapValues(data.scriptNameTags(), this::getPeelScript),
                documentPersistence.getData().template(),
                renderConfigurationRepository.findById(data.renderConfigurationId().id()).orElseThrow(() -> new ResourceNotFoundException("Render configuration not found: " + data.renderConfigurationId().id())).getExpressionRenderConfiguration(),
                documentPersistence.getData().localOverrides()
        );
        String html = documentRenderService.render(document, bindings);
        return new DocumentPreviewResponse(html);
    }

    private PeelScript getPeelScript(PeelScriptId scriptId) {
        return peelScriptRepository.findById(scriptId.id())
                .orElseThrow(() -> new ResourceNotFoundException("Did not find Script with Id" + scriptId)).getPeelScript();
    }

    private DocumentContent toContent(DocumentPersistence document) {
        return new DocumentContent(
                document.getId(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    @Override
    public void deleteDocument(String id) {
        peelDocumentRepository.deleteById(id);
    }

    private DocumentSummaryResponse toSummary(DocumentPersistence document) {
        return new DocumentSummaryResponse(
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
